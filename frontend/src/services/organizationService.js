import api from './api';

export const getMyOrganizations = async () => {
  const response = await api.get('/organizations');
  return response.data;
};

export const getOrganizationById = async (organizationId) => {
  const response = await api.get(`/organizations/${organizationId}`);
  return response.data;
};
