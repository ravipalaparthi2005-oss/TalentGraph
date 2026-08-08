import api from './api';

export const documentService = {
  uploadDocument: async (candidateId, file, documentType = 'RESUME', applicationId = null) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    if (applicationId) {
      formData.append('applicationId', applicationId);
    }

    const response = await api.post(`/candidates/${candidateId}/documents`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  getCandidateDocuments: async (candidateId) => {
    const response = await api.get(`/candidates/${candidateId}/documents`);
    return response.data;
  },

  getDocumentById: async (candidateId, documentId) => {
    const response = await api.get(`/candidates/${candidateId}/documents/${documentId}`);
    return response.data;
  },

  downloadDocument: async (candidateId, documentId, filename) => {
    const response = await api.get(`/candidates/${candidateId}/documents/${documentId}/download`, {
      responseType: 'blob',
    });
    
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename || 'document.pdf');
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  }
};

export default documentService;
