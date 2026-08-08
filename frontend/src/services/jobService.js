import api from './api';

export const createJob = async (jobData, organizationId) => {
  const response = await api.post('/jobs', jobData, { params: { organizationId } });
  return response.data;
};

export const getJobs = async ({ organizationId, status, employmentType, search, page = 0, size = 20, sortBy = 'createdAt', sortDirection = 'DESC' }) => {
  const params = { organizationId, page, size, sortBy, sortDirection };
  if (status && status !== 'ALL') params.status = status;
  if (employmentType && employmentType !== 'ALL') params.employmentType = employmentType;
  if (search) params.search = search;

  const response = await api.get('/jobs', { params });
  return response.data;
};

export const getJobById = async (jobId) => {
  const response = await api.get(`/jobs/${jobId}`);
  return response.data;
};

export const updateJob = async (jobId, jobData) => {
  const response = await api.put(`/jobs/${jobId}`, jobData);
  return response.data;
};

export const updateJobStatus = async (jobId, status) => {
  const response = await api.patch(`/jobs/${jobId}/status`, { status });
  return response.data;
};

export const deleteJob = async (jobId) => {
  const response = await api.delete(`/jobs/${jobId}`);
  return response.data;
};

// Job Requirements
export const getJobRequirements = async (jobId) => {
  const response = await api.get(`/jobs/${jobId}/requirements`);
  return response.data;
};

export const addJobRequirement = async (jobId, requirementData) => {
  const response = await api.post(`/jobs/${jobId}/requirements`, requirementData);
  return response.data;
};

export const updateJobRequirement = async (jobId, requirementId, requirementData) => {
  const response = await api.put(`/jobs/${jobId}/requirements/${requirementId}`, requirementData);
  return response.data;
};

export const deleteJobRequirement = async (jobId, requirementId) => {
  const response = await api.delete(`/jobs/${jobId}/requirements/${requirementId}`);
  return response.data;
};

// Evaluation Criteria
export const getJobCriteria = async (jobId) => {
  const response = await api.get(`/jobs/${jobId}/criteria`);
  return response.data;
};

export const addJobCriterion = async (jobId, criterionData) => {
  const response = await api.post(`/jobs/${jobId}/criteria`, criterionData);
  return response.data;
};

export const updateJobCriterion = async (jobId, criterionId, criterionData) => {
  const response = await api.put(`/jobs/${jobId}/criteria/${criterionId}`, criterionData);
  return response.data;
};

export const deleteJobCriterion = async (jobId, criterionId) => {
  const response = await api.delete(`/jobs/${jobId}/criteria/${criterionId}`);
  return response.data;
};

// Job Audit Activity
export const getJobActivity = async (jobId) => {
  const response = await api.get(`/jobs/${jobId}/activity`);
  return response.data;
};
