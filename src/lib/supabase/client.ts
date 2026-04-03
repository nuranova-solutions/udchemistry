import { createClient } from "@supabase/supabase-js";

const supabaseUrl =
  import.meta.env.VITE_SUPABASE_URL ?? "https://qxvwnqsrkimzenxzjqys.supabase.co";
const supabasePublishableKey =
  import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY ?? "sb_publishable_orlhuTyVCjOo7gq4jCP_7Q_x8qLo-A0";

export const supabase = createClient(supabaseUrl, supabasePublishableKey);
