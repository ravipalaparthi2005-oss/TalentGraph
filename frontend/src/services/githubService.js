import api from './api';

const githubService = {
  /**
   * Get candidate's GitHub connection status.
   */
  async getGithubStatus(candidateId) {
    const response = await api.get(`/candidates/${candidateId}/github`);
    return response.data;
  },

  /**
   * Get GitHub OAuth authorization URL for candidate.
   */
  async connectGithub(candidateId) {
    const response = await api.get(`/candidates/${candidateId}/github/connect`);
    return response.data;
  },

  /**
   * Trigger GitHub evidence synchronization.
   */
  async syncGithub(candidateId) {
    const response = await api.post(`/candidates/${candidateId}/github/sync`);
    return response.data;
  },

  /**
   * List candidate's synchronized GitHub repositories.
   */
  async getRepositories(candidateId) {
    const response = await api.get(`/candidates/${candidateId}/github/repositories`);
    return response.data;
  },

  /**
   * Disconnect GitHub identity from candidate.
   */
  async disconnectGithub(candidateId) {
    const response = await api.delete(`/candidates/${candidateId}/github`);
    return response.data;
  },
};

export default githubService;
