import jsQR from "jsqr";
import { useCallback, useEffect, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Camera, CameraOff } from "lucide-react";
import { DataTable } from "../../components/ui/DataTable";
import { PageHeader } from "../../components/ui/PageHeader";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  createAttendance,
  deleteAttendance,
  fetchAttendance,
  fetchStudents,
  markCurrentMonthPaid,
  scanAttendance,
  type AttendanceFormValues,
  updateAttendance,
} from "../../lib/api";
import { formatDate } from "../../lib/utils/formatters";
import type { AttendanceRecord, ScanAttendanceResult } from "../../types/app";
import { useAuth } from "../auth/useAuth";
import { ScanResultPanel } from "./ScanResultPanel";

function todayString() {
  return new Date().toISOString().slice(0, 10);
}

function getCameraErrorMessage(error: unknown) {
  if (error instanceof DOMException) {
    if (error.name === "NotAllowedError") {
      return "Allow camera access to scan student QR codes.";
    }

    if (error.name === "NotFoundError") {
      return "No camera was found on this device.";
    }

    if (error.name === "NotReadableError") {
      return "The camera is already being used by another app.";
    }
  }

  return "Unable to start the camera. You can still paste the student ID below.";
}

const emptyForm: AttendanceFormValues = {
  student_id: "",
  attendance_date: todayString(),
  status: "present",
};

export function AttendancePage() {
  const { profile } = useAuth();
  const queryClient = useQueryClient();
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const animationFrameRef = useRef<number | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const cameraSessionRef = useRef(0);
  const scannerLockedRef = useRef(false);
  const lastDetectedRef = useRef<{ value: string; time: number } | null>(null);
  const pendingScanRef = useRef(false);
  const currentResultRef = useRef<ScanAttendanceResult | null>(null);
  const mutateScanRef = useRef<(value: string) => void>(() => undefined);
  const cameraEnabledRef = useRef(true);
  const [qrValue, setQrValue] = useState("");
  const [scanResult, setScanResult] = useState<ScanAttendanceResult | null>(null);
  const [paymentStatus, setPaymentStatus] = useState<"idle" | "paid" | "skipped">("idle");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [editingAttendance, setEditingAttendance] = useState<AttendanceRecord | null>(null);
  const [formValues, setFormValues] = useState<AttendanceFormValues>(emptyForm);
  const [dateFilter, setDateFilter] = useState("all");
  const [cameraEnabled, setCameraEnabled] = useState(true);
  const [cameraState, setCameraState] = useState<"idle" | "starting" | "ready" | "error" | "unsupported">("idle");
  const [cameraMessage, setCameraMessage] = useState("Preparing camera access...");

  const attendanceQuery = useQuery({
    queryKey: ["attendance", profile?.id, profile?.institute_id],
    queryFn: () => fetchAttendance(profile!),
    enabled: Boolean(profile),
  });

  const studentsQuery = useQuery({
    queryKey: ["students", profile?.id, profile?.institute_id],
    queryFn: () => fetchStudents(profile!),
    enabled: Boolean(profile),
  });

  useEffect(() => {
    pendingScanRef.current = false;
    currentResultRef.current = scanResult;
  }, [scanResult]);

  useEffect(() => {
    cameraEnabledRef.current = cameraEnabled;
  }, [cameraEnabled]);

  useEffect(() => {
    if (editingAttendance) {
      return;
    }

    if (!formValues.student_id && studentsQuery.data?.[0]?.id) {
      setFormValues((currentValues) => ({
        ...currentValues,
        student_id: studentsQuery.data?.[0]?.id ?? "",
      }));
    }
  }, [editingAttendance, formValues.student_id, studentsQuery.data]);

  const releaseCamera = useCallback(() => {
    cameraSessionRef.current += 1;

    if (animationFrameRef.current !== null) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }

    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }

    if (videoRef.current) {
      videoRef.current.pause();
      videoRef.current.srcObject = null;
    }
  }, []);

  const scanMutation = useMutation({
    mutationFn: (value: string) => scanAttendance(profile!, value.trim()),
    onSuccess: (result) => {
      releaseCamera();
      pendingScanRef.current = false;
      currentResultRef.current = result;
      setScanResult(result);
      setPaymentStatus("idle");
      setCameraState("idle");
      setCameraMessage("Camera paused after scan. Click Done to scan the next student.");
      setFeedback(
        result.duplicate
          ? "Attendance was already recorded for today."
          : "Attendance marked successfully.",
      );
      void queryClient.invalidateQueries({ queryKey: ["attendance"] });
      void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
    onError: (error) => {
      pendingScanRef.current = false;
      scannerLockedRef.current = false;
      setFeedback(error instanceof Error ? error.message : "Unable to process scan.");
    },
  });

  useEffect(() => {
    pendingScanRef.current = scanMutation.isPending;
  }, [scanMutation.isPending]);

  useEffect(() => {
    mutateScanRef.current = scanMutation.mutate;
  }, [scanMutation.mutate]);

  const processScanValue = useCallback(
    (value: string) => {
      const normalizedValue = value.trim();

      if (!normalizedValue || pendingScanRef.current || currentResultRef.current) {
        return;
      }

      const now = Date.now();
      const lastDetected = lastDetectedRef.current;

      if (lastDetected && lastDetected.value === normalizedValue && now - lastDetected.time < 2500) {
        return;
      }

      scannerLockedRef.current = true;
      lastDetectedRef.current = {
        value: normalizedValue,
        time: now,
      };
      setQrValue(normalizedValue);
      setFeedback("QR detected. Processing attendance...");
      mutateScanRef.current(normalizedValue);
    },
    [],
  );

  const startCamera = useCallback(async () => {
    if (currentResultRef.current) {
      return;
    }

    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraState("unsupported");
      setCameraMessage("Camera scanning needs a browser with webcam access support.");
      return;
    }

    releaseCamera();
    const sessionId = cameraSessionRef.current;
    setCameraState("starting");
    setCameraMessage("Requesting camera access...");

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: {
          facingMode: { ideal: "environment" },
        },
      });

      if (!videoRef.current) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }

      if (
        cameraSessionRef.current !== sessionId ||
        !cameraEnabledRef.current ||
        currentResultRef.current
      ) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }

      streamRef.current = stream;
      videoRef.current.srcObject = stream;
      await videoRef.current.play();

      const canvas = document.createElement("canvas");
      const context = canvas.getContext("2d", { willReadFrequently: true });

      if (!context) {
        throw new Error("Unable to prepare the camera scanner.");
      }

      setCameraState("ready");
      setCameraMessage("Camera is live. Hold the student QR inside the frame.");

      const scanFrame = () => {
        const video = videoRef.current;

        if (
          cameraSessionRef.current !== sessionId ||
          !cameraEnabledRef.current ||
          currentResultRef.current
        ) {
          return;
        }

        if (!video || !streamRef.current) {
          return;
        }

        if (
          !scannerLockedRef.current &&
          !pendingScanRef.current &&
          !currentResultRef.current &&
          video.readyState >= HTMLMediaElement.HAVE_ENOUGH_DATA &&
          video.videoWidth > 0 &&
          video.videoHeight > 0
        ) {
          canvas.width = video.videoWidth;
          canvas.height = video.videoHeight;
          context.drawImage(video, 0, 0, canvas.width, canvas.height);

          const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
          const code = jsQR(imageData.data, imageData.width, imageData.height, {
            inversionAttempts: "attemptBoth",
          });

          if (code?.data) {
            processScanValue(code.data);
          }
        }

        animationFrameRef.current = requestAnimationFrame(scanFrame);
      };

      animationFrameRef.current = requestAnimationFrame(scanFrame);
    } catch (error) {
      releaseCamera();
      setCameraState("error");
      setCameraMessage(getCameraErrorMessage(error));
    }
  }, [processScanValue, releaseCamera]);

  useEffect(() => {
    if (!cameraEnabled || scanResult) {
      releaseCamera();

      if (!cameraEnabled) {
        setCameraState("idle");
        setCameraMessage("Camera stopped. You can start it again anytime.");
      }

      return;
    }

    void startCamera();

    return () => {
      releaseCamera();
    };
  }, [cameraEnabled, releaseCamera, scanResult, startCamera]);

  const paymentMutation = useMutation({
    mutationFn: (studentId: string) => markCurrentMonthPaid(profile!, studentId),
    onSuccess: () => {
      setPaymentStatus("paid");
      setFeedback("Payment marked for the current month.");
      void queryClient.invalidateQueries({ queryKey: ["payments"] });
      void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  const saveMutation = useMutation({
    mutationFn: async (values: AttendanceFormValues) => {
      if (editingAttendance) {
        await updateAttendance(profile!, editingAttendance.id, values);
        return;
      }

      await createAttendance(profile!, values);
    },
    onSuccess: async () => {
      setFeedback(editingAttendance ? "Attendance updated successfully." : "Attendance added successfully.");
      setEditingAttendance(null);
      setFormValues({
        ...emptyForm,
        student_id: studentsQuery.data?.[0]?.id ?? "",
      });
      await queryClient.invalidateQueries({ queryKey: ["attendance"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to save attendance.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (attendanceId: string) => deleteAttendance(attendanceId),
    onSuccess: async () => {
      setFeedback("Attendance deleted successfully.");
      setEditingAttendance(null);
      setFormValues({
        ...emptyForm,
        student_id: studentsQuery.data?.[0]?.id ?? "",
      });
      await queryClient.invalidateQueries({ queryKey: ["attendance"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to delete attendance.");
    },
  });

  function handleChange<K extends keyof AttendanceFormValues>(key: K, value: AttendanceFormValues[K]) {
    setFormValues((currentValues) => ({
      ...currentValues,
      [key]: value,
    }));
  }

  function handleEdit(record: AttendanceRecord) {
    setEditingAttendance(record);
    setFormValues({
      student_id: record.student_id,
      attendance_date: record.attendance_date,
      status: record.status,
    });
    setFeedback(null);
  }

  function handleReset() {
    setEditingAttendance(null);
    setFormValues({
      ...emptyForm,
      student_id: studentsQuery.data?.[0]?.id ?? "",
    });
    setFeedback(null);
  }

  function handleManualScan() {
    scannerLockedRef.current = true;
    processScanValue(qrValue);
  }

  const attendanceRows = attendanceQuery.data ?? [];
  const visibleAttendance = attendanceRows.filter((record) => {
    if (dateFilter === "all") {
      return true;
    }

    return record.attendance_date === dateFilter;
  });

  return (
    <div className="page-stack">
      <PageHeader
        title="Attendance"
        description="Use the webcam to scan student QR codes, then add, edit, save, or delete attendance records when corrections are needed."
      />

      <div className="scan-shell">
        <SectionCard title="QR scanner" description="Open the webcam, hold the student QR inside the frame, and attendance will mark automatically.">
          {!scanResult ? (
            <div className="scan-panel">
              <div className={`scan-preview ${cameraState === "ready" ? "live" : ""}`}>
                <video ref={videoRef} className="scan-video" autoPlay muted playsInline />

                {cameraState === "ready" ? (
                  <div className="scan-overlay">Align the QR inside the frame</div>
                ) : (
                  <div className="scan-placeholder">
                    <strong>
                      {cameraState === "starting"
                        ? "Starting camera..."
                        : cameraState === "error"
                          ? "Camera unavailable"
                          : cameraState === "unsupported"
                            ? "Camera not supported"
                            : "Ready to scan"}
                    </strong>
                    <span>{cameraMessage}</span>
                  </div>
                )}
              </div>

              <div className="scan-actions">
                {cameraEnabled ? (
                  <button
                    className="button secondary"
                    type="button"
                    onClick={() => {
                      setCameraEnabled(false);
                    }}
                  >
                    <CameraOff size={16} />
                    Stop camera
                  </button>
                ) : (
                  <button
                    className="button secondary"
                    type="button"
                    onClick={() => {
                      scannerLockedRef.current = false;
                      setCameraEnabled(true);
                    }}
                  >
                    <Camera size={16} />
                    Start camera
                  </button>
                )}
              </div>

              <input
                className="input"
                value={qrValue}
                onChange={(event) => setQrValue(event.target.value)}
                placeholder="Scan or paste student ID"
              />
              <button
                className="button"
                type="button"
                onClick={handleManualScan}
                disabled={!qrValue.trim() || scanMutation.isPending}
              >
                {scanMutation.isPending ? "Processing..." : "Scan QR"}
              </button>
            </div>
          ) : (
            <ScanResultPanel
              result={scanResult}
              paymentStatus={paymentStatus}
              paymentLoading={paymentMutation.isPending}
              onMarkPaid={() => paymentMutation.mutate(scanResult.student.id)}
              onSkip={() => {
                setPaymentStatus("skipped");
                setFeedback("Payment was skipped for this scan.");
              }}
              onDone={() => {
                setQrValue("");
                setScanResult(null);
                currentResultRef.current = null;
                scannerLockedRef.current = false;
                lastDetectedRef.current = null;
                setPaymentStatus("idle");
                setFeedback(null);
              }}
            />
          )}

          {feedback ? (
            <p className={feedback.toLowerCase().includes("unable") ? "error-text" : "helper-text"}>
              {feedback}
            </p>
          ) : null}
        </SectionCard>

        <SectionCard
          title={editingAttendance ? "Edit attendance" : "Add attendance"}
          description="Use this form for manual entries or corrections after a class."
        >
          <form
            className="management-form"
            onSubmit={(event) => {
              event.preventDefault();
              saveMutation.mutate(formValues);
            }}
          >
            <div className="form-grid">
              <div>
                <label className="field-label" htmlFor="attendance_student">
                  Student
                </label>
                <select
                  id="attendance_student"
                  className="input"
                  value={formValues.student_id}
                  onChange={(event) => handleChange("student_id", event.target.value)}
                  required
                >
                  <option value="" disabled>
                    Select student
                  </option>
                  {(studentsQuery.data ?? []).map((student) => (
                    <option key={student.id} value={student.id}>
                      {student.full_name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="field-label" htmlFor="attendance_date">
                  Date
                </label>
                <input
                  id="attendance_date"
                  className="input"
                  type="date"
                  value={formValues.attendance_date}
                  onChange={(event) => handleChange("attendance_date", event.target.value)}
                  required
                />
              </div>

              <div>
                <label className="field-label" htmlFor="attendance_status">
                  Status
                </label>
                <select
                  id="attendance_status"
                  className="input"
                  value={formValues.status}
                  onChange={(event) =>
                    handleChange("status", event.target.value as AttendanceFormValues["status"])
                  }
                >
                  <option value="present">Present</option>
                  <option value="late">Late</option>
                  <option value="absent">Absent</option>
                </select>
              </div>
            </div>

            <div className="inline-actions">
              <button className="button" type="submit" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? "Saving..." : editingAttendance ? "Save changes" : "Save attendance"}
              </button>
              <button className="button ghost" type="button" onClick={handleReset}>
                Clear
              </button>
            </div>
          </form>
        </SectionCard>
      </div>

      <SectionCard
        title="Attendance records"
        description="Admin and staff can review the latest attendance rows inside their access scope."
        actions={
          <select
            className="input filter-input"
            value={dateFilter}
            onChange={(event) => setDateFilter(event.target.value)}
          >
            <option value="all">All dates</option>
            {Array.from(new Set(attendanceRows.map((record) => record.attendance_date))).map((date) => (
              <option key={date} value={date}>
                {formatDate(date)}
              </option>
            ))}
          </select>
        }
      >
        <DataTable
          rows={visibleAttendance}
          getRowKey={(row) => row.id}
          columns={[
            { header: "Student", render: (row) => row.students?.full_name ?? "-" },
            { header: "Date", render: (row) => formatDate(row.attendance_date) },
            {
              header: "Status",
              render: (row) => (
                <StatusBadge
                  label={row.status}
                  tone={row.status === "present" ? "success" : "warning"}
                />
              ),
            },
            { header: "Marked at", render: (row) => formatDate(row.marked_at, "dd MMM yyyy, hh:mm a") },
            {
              header: "Actions",
              render: (row) => (
                <div className="table-actions">
                  <button className="button ghost small-button" type="button" onClick={() => handleEdit(row)}>
                    Edit
                  </button>
                  <button
                    className="button ghost danger-button small-button"
                    type="button"
                    onClick={() => {
                      const confirmed = window.confirm("Delete this attendance record?");

                      if (confirmed) {
                        deleteMutation.mutate(row.id);
                      }
                    }}
                    disabled={deleteMutation.isPending}
                  >
                    Delete
                  </button>
                </div>
              ),
            },
          ]}
        />
      </SectionCard>
    </div>
  );
}
