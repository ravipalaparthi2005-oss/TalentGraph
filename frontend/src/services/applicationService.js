import api from './api';

export const applicationService = {
  createApplication: async (jobId, applicationData) => {
    const response = await api.post(`/jobs/${jobId}/applications`, applicationData);
    return response.data;
  },

  getJobApplications: async (jobId) => {
    const response = await api.get(`/jobs/${jobId}/applications`);
    return response.data;
  },

  getCandidateApplications: async (candidateId) => {
    const response = await api.get(`/candidates/${candidateId}/applications`);
    return response.data;
  },

  getApplicationById: async (applicationId) => {
    const response = await api.get(`/applications/${applicationId}`);
    return response.data;
  },

  updateApplicationStatus: async (applicationId, status) => {
    const response = await api.patch(`/applications/${applicationId}/status`, { status });
    return response.data;
  }
};

export default applicationService;
