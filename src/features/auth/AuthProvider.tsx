import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { Session, User } from "@supabase/supabase-js";
import { supabase } from "../../lib/supabase/client";
import type { Profile } from "../../types/app";

interface AuthContextValue {
  session: Session | null;
  user: User | null;
  profile: Profile | null;
  isLoading: boolean;
  signIn: (identifier: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

async function fetchProfile(userId: string) {
  const { data, error } = await supabase
    .from("profiles")
    .select("id, full_name, email, username, role, institute_id, phone, status")
    .eq("id", userId)
    .single();

  if (error) {
    throw error;
  }

  return data as Profile;
}

function assertActiveProfile(profile: Profile) {
  if (profile.status !== "active") {
    throw new Error("This account is inactive. Please contact the admin.");
  }
}

async function resolveEmailFromIdentifier(identifier: string) {
  if (identifier.includes("@")) {
    return identifier;
  }

  const { data, error } = await supabase.rpc("get_login_email_by_username", {
    p_username: identifier,
  });

  if (error) {
    throw error;
  }

  if (!data) {
    throw new Error("Username was not found.");
  }

  return data as string;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshProfile = useCallback(async () => {
    if (!user) {
      setProfile(null);
      return;
    }

    try {
      const nextProfile = await fetchProfile(user.id);
      assertActiveProfile(nextProfile);
      setProfile(nextProfile);
    } catch {
      setProfile(null);
    }
  }, [user]);

  useEffect(() => {
    let isMounted = true;

    // Bootstrap auth state before protected routes render.
    async function initialise() {
      const {
        data: { session: existingSession },
      } = await supabase.auth.getSession();

      if (!isMounted) {
        return;
      }

      setSession(existingSession);
      setUser(existingSession?.user ?? null);

      if (existingSession?.user) {
        try {
          const nextProfile = await fetchProfile(existingSession.user.id);
          assertActiveProfile(nextProfile);
          if (isMounted) {
            setProfile(nextProfile);
          }
        } catch {
          await supabase.auth.signOut();
          if (isMounted) {
            setProfile(null);
          }
        }
      }

      if (isMounted) {
        setIsLoading(false);
      }
    }

    void initialise();

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      setSession(nextSession);
      setUser(nextSession?.user ?? null);

      if (!nextSession?.user) {
        setProfile(null);
        setIsLoading(false);
        return;
      }

      void fetchProfile(nextSession.user.id)
        .then((nextProfile) => {
          assertActiveProfile(nextProfile);
          setProfile(nextProfile);
        })
        .catch(async () => {
          await supabase.auth.signOut();
          setProfile(null);
        })
        .finally(() => {
          setIsLoading(false);
        });
    });

    return () => {
      isMounted = false;
      subscription.unsubscribe();
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      user,
      profile,
      isLoading,
      signIn: async (identifier, password) => {
        const email = await resolveEmailFromIdentifier(identifier);
        const { error } = await supabase.auth.signInWithPassword({ email, password });

        if (error) {
          throw error;
        }

        const {
          data: { user: signedInUser },
        } = await supabase.auth.getUser();

        if (!signedInUser) {
          throw new Error("Unable to load the signed-in user.");
        }

        const nextProfile = await fetchProfile(signedInUser.id);

        try {
          assertActiveProfile(nextProfile);
        } catch (error) {
          await supabase.auth.signOut();
          throw error;
        }
      },
      signOut: async () => {
        const { error } = await supabase.auth.signOut();

        if (error) {
          throw error;
        }
      },
      refreshProfile,
    }),
    [session, user, profile, isLoading, refreshProfile],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
