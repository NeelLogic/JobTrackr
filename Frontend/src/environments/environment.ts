type JobTrackrRuntimeConfig = {
  apiUrl?: string;
};

const runtimeConfig = (
  globalThis as typeof globalThis & {
    __JOBTRACKR_CONFIG__?: JobTrackrRuntimeConfig;
  }
).__JOBTRACKR_CONFIG__;

export const environment = {
  production: false,
  apiUrl: runtimeConfig?.apiUrl?.replace(/\/+$/, '') || '/api',
};
