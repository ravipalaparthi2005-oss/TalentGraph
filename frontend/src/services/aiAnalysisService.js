import api from './api';

const aiAnalysisService = {
  /**
   * Trigger AI analysis for a candidate document.
   * @param {string} candidateId
   * @param {string} documentId
   * @param {boolean} reanalyze - force a new run even if cached result exists
   */
  async triggerAnalysis(candidateId, documentId, reanalyze = false) {
    const response = await api.post(
      `/candidates/${candidateId}/documents/${documentId}/ai-analysis`,
      {},
      { params: { reanalyze } }
    );
    return response.data;
  },

  /**
   * Get the latest AI analysis status and results for a document.
   * @param {string} candidateId
   * @param {string} documentId
   */
  async getAnalysis(candidateId, documentId) {
    const response = await api.get(
      `/candidates/${candidateId}/documents/${documentId}/ai-analysis`
    );
    return response.data;
  },
};

export default aiAnalysisService;
