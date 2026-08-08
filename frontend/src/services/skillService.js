import api from './api';

export const getSkills = async (search = '') => {
  const response = await api.get('/skills', { params: { search } });
  return response.data;
};

export const createSkill = async (skillData) => {
  const response = await api.post('/skills', skillData);
  return response.data;
};

export const getSkillById = async (skillId) => {
  const response = await api.get(`/skills/${skillId}`);
  return response.data;
};
