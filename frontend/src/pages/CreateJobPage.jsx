import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import RecruiterShell from '../components/RecruiterShell';
import { useAuth } from '../context/AuthContext';
import { createJob, updateJobStatus, addJobRequirement, addJobCriterion } from '../services/jobService';
import { getSkills, createSkill } from '../services/skillService';
import { ArrowLeft, Plus, Trash2, CheckCircle2, Save, Rocket, AlertCircle, Loader2, Sparkles, Layers } from 'lucide-react';

const CreateJobPage = () => {
  const { activeOrganization } = useAuth();
  const navigate = useNavigate();

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Form State
  const [title, setTitle] = useState('');
  const [department, setDepartment] = useState('Engineering');
  const [location, setLocation] = useState('Remote');
  const [employmentType, setEmploymentType] = useState('FULL_TIME');
  const [description, setDescription] = useState('');

  // Skill Catalog state
  const [availableSkills, setAvailableSkills] = useState([]);
  const [newSkillName, setNewSkillName] = useState('');

  // Requirements List State
  const [requirements, setRequirements] = useState([
    { name: 'Core Language Proficiency', description: 'Deep knowledge of primary language idioms and paradigms', requirementType: 'REQUIRED', importance: 'CRITICAL', minimumLevel: 'Senior', skillId: '' }
  ]);

  // Evaluation Criteria List State
  const [criteria, setCriteria] = useState([
    { name: 'Technical Depth & Architecture', description: 'System design and clean code standards', weight: '0.60', skillId: '' },
    { name: 'Execution & Quality', description: 'Test coverage and error resilience', weight: '0.40', skillId: '' }
  ]);

  useEffect(() => {
    fetchSkillsCatalog();
  }, []);

  const fetchSkillsCatalog = async () => {
    try {
      const res = await getSkills();
      if (res?.data) {
        setAvailableSkills(res.data);
      }
    } catch (err) {
      // Catalog might start empty
    }
  };

  const handleCreateNewSkill = async () => {
    if (!newSkillName.trim()) return;
    try {
      const res = await createSkill({ name: newSkillName.trim(), category: 'OTHER' });
      if (res?.data) {
        setAvailableSkills((prev) => [...prev, res.data]);
        setNewSkillName('');
      }
    } catch (err) {
      setError(err.message || 'Failed to create skill record.');
    }
  };

  const addRequirementRow = () => {
    setRequirements((prev) => [
      ...prev,
      { name: '', description: '', requirementType: 'REQUIRED', importance: 'HIGH', minimumLevel: 'Mid', skillId: '' }
    ]);
  };

  const removeRequirementRow = (index) => {
    setRequirements((prev) => prev.filter((_, i) => i !== index));
  };

  const updateRequirementField = (index, field, value) => {
    setRequirements((prev) => {
      const updated = [...prev];
      updated[index][field] = value;
      return updated;
    });
  };

  const addCriterionRow = () => {
    setCriteria((prev) => [
      ...prev,
      { name: '', description: '', weight: '0.20', skillId: '' }
    ]);
  };

  const removeCriterionRow = (index) => {
    setCriteria((prev) => prev.filter((_, i) => i !== index));
  };

  const updateCriterionField = (index, field, value) => {
    setCriteria((prev) => {
      const updated = [...prev];
      updated[index][field] = value;
      return updated;
    });
  };

  const totalCriteriaWeight = criteria.reduce((sum, c) => sum + (parseFloat(c.weight) || 0), 0);

  const handleSubmit = async (publishNow = false) => {
    if (!title.trim() || !description.trim()) {
      setError('Job Title and Description are required.');
      return;
    }

    if (publishNow) {
      if (requirements.length === 0) {
        setError('A job must have at least one requirement before publishing.');
        return;
      }
      const hasRequired = requirements.some((r) => r.requirementType === 'REQUIRED');
      if (!hasRequired) {
        setError('At least one requirement must be set to REQUIRED before publishing.');
        return;
      }
      if (criteria.length > 0 && Math.abs(totalCriteriaWeight - 1.0) > 0.001) {
        setError(`Evaluation criteria weights must sum to 1.00 before publishing (current sum: ${totalCriteriaWeight.toFixed(2)}).`);
        return;
      }
    }

    setError(null);
    setSubmitting(true);

    try {
      // 1. Create Job in DRAFT mode
      const jobRes = await createJob({
        title: title.trim(),
        department: department.trim(),
        location: location.trim(),
        employmentType,
        description: description.trim(),
        organizationId: activeOrganization.organizationId
      }, activeOrganization.organizationId);

      const createdJobId = jobRes.data.id;

      // 2. Add Requirements
      for (const req of requirements) {
        if (req.name.trim()) {
          await addJobRequirement(createdJobId, {
            name: req.name.trim(),
            description: req.description.trim(),
            requirementType: req.requirementType,
            importance: req.importance,
            minimumLevel: req.minimumLevel,
            skillId: req.skillId || null
          });
        }
      }

      // 3. Add Criteria
      for (const crit of criteria) {
        if (crit.name.trim()) {
          await addJobCriterion(createdJobId, {
            name: crit.name.trim(),
            description: crit.description.trim(),
            weight: parseFloat(crit.weight) || 0.1,
            skillId: crit.skillId || null
          });
        }
      }

      // 4. If publishNow requested, transition status to OPEN
      if (publishNow) {
        await updateJobStatus(createdJobId, 'OPEN');
      }

      navigate(`/app/jobs/${createdJobId}`);
    } catch (err) {
      setError(err.message || 'Failed to persist job record.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <RecruiterShell>
      <div className="max-w-4xl mx-auto space-y-6 pb-12">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link to="/app/jobs" className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors">
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <div>
              <h1 className="text-xl font-bold text-white tracking-tight">Create Engineering Job</h1>
              <p className="text-xs text-slate-400">Define job requirements, skills, and evaluation criteria</p>
            </div>
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-3">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Form Container */}
        <div className="space-y-6">
          {/* Section 1: Basic Information */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-4 backdrop-blur-sm">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider text-emerald-400">1. Basic Job Details</h2>

            <div>
              <label htmlFor="title" className="block text-xs font-medium text-slate-300 mb-1">
                Job Title <span className="text-rose-400">*</span>
              </label>
              <input
                id="title"
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g. Senior Backend Engineer"
                className="w-full px-3.5 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500/80 transition-colors"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label htmlFor="department" className="block text-xs font-medium text-slate-300 mb-1">Department</label>
                <input
                  id="department"
                  type="text"
                  value={department}
                  onChange={(e) => setDepartment(e.target.value)}
                  placeholder="Engineering"
                  className="w-full px-3.5 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500/80 transition-colors"
                />
              </div>

              <div>
                <label htmlFor="location" className="block text-xs font-medium text-slate-300 mb-1">Location</label>
                <input
                  id="location"
                  type="text"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  placeholder="Remote / San Francisco"
                  className="w-full px-3.5 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500/80 transition-colors"
                />
              </div>

              <div>
                <label htmlFor="employmentType" className="block text-xs font-medium text-slate-300 mb-1">Employment Type</label>
                <select
                  id="employmentType"
                  value={employmentType}
                  onChange={(e) => setEmploymentType(e.target.value)}
                  className="w-full px-3.5 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-emerald-500/80 transition-colors cursor-pointer"
                >
                  <option value="FULL_TIME">Full Time</option>
                  <option value="PART_TIME">Part Time</option>
                  <option value="CONTRACT">Contract</option>
                  <option value="INTERNSHIP">Internship</option>
                </select>
              </div>
            </div>

            <div>
              <label htmlFor="description" className="block text-xs font-medium text-slate-300 mb-1">
                Job Description <span className="text-rose-400">*</span>
              </label>
              <textarea
                id="description"
                rows={4}
                required
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Describe role responsibilities, team architecture, tech stack expectations..."
                className="w-full px-3.5 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500/80 transition-colors"
              />
            </div>
          </div>

          {/* Section 2: Skill Catalog & Requirements */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-4 backdrop-blur-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-bold text-white uppercase tracking-wider text-emerald-400">2. Job Requirements & Skill Catalog</h2>
              <button
                type="button"
                onClick={addRequirementRow}
                className="flex items-center gap-1 text-xs font-semibold text-emerald-400 hover:text-emerald-300 bg-emerald-500/10 border border-emerald-500/20 px-3 py-1.5 rounded-lg transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Requirement</span>
              </button>
            </div>

            {/* Quick Skill Creator */}
            <div className="p-3 bg-slate-950/60 border border-slate-800/80 rounded-xl flex items-center gap-2">
              <span className="text-xs text-slate-400 whitespace-nowrap">Catalog Skill:</span>
              <input
                type="text"
                value={newSkillName}
                onChange={(e) => setNewSkillName(e.target.value)}
                placeholder="e.g. Spring Boot, PostgreSQL, Docker"
                className="flex-1 px-3 py-1 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-emerald-500/60"
              />
              <button
                type="button"
                onClick={handleCreateNewSkill}
                className="px-3 py-1 bg-slate-800 hover:bg-slate-700 text-xs font-medium text-slate-200 rounded-lg transition-colors cursor-pointer"
              >
                Add Skill
              </button>
            </div>

            {/* Requirements Items */}
            <div className="space-y-3">
              {requirements.map((req, idx) => (
                <div key={idx} className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl space-y-3 relative group">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    <input
                      type="text"
                      value={req.name}
                      onChange={(e) => updateRequirementField(idx, 'name', e.target.value)}
                      placeholder="Requirement Title (e.g. Java 21 Mastery)"
                      className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500/60"
                    />

                    <select
                      value={req.requirementType}
                      onChange={(e) => updateRequirementField(idx, 'requirementType', e.target.value)}
                      className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-200 focus:outline-none cursor-pointer"
                    >
                      <option value="REQUIRED">REQUIRED</option>
                      <option value="PREFERRED">PREFERRED</option>
                    </select>

                    <select
                      value={req.importance}
                      onChange={(e) => updateRequirementField(idx, 'importance', e.target.value)}
                      className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-200 focus:outline-none cursor-pointer"
                    >
                      <option value="CRITICAL">Importance: CRITICAL</option>
                      <option value="HIGH">Importance: HIGH</option>
                      <option value="MEDIUM">Importance: MEDIUM</option>
                      <option value="LOW">Importance: LOW</option>
                    </select>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <select
                      value={req.skillId}
                      onChange={(e) => updateRequirementField(idx, 'skillId', e.target.value)}
                      className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-200 focus:outline-none cursor-pointer"
                    >
                      <option value="">-- Associate Skill (Optional) --</option>
                      {availableSkills.map((s) => (
                        <option key={s.id} value={s.id}>{s.name} ({s.normalizedName})</option>
                      ))}
                    </select>

                    <input
                      type="text"
                      value={req.description}
                      onChange={(e) => updateRequirementField(idx, 'description', e.target.value)}
                      placeholder="Requirement Description / Expectation details..."
                      className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500/60"
                    />
                  </div>

                  {requirements.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeRequirementRow(idx)}
                      className="absolute top-3 right-3 text-slate-500 hover:text-rose-400 p-1 transition-colors cursor-pointer"
                      title="Delete requirement"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Section 3: Evaluation Criteria */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-4 backdrop-blur-sm">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-sm font-bold text-white uppercase tracking-wider text-emerald-400">3. Evaluation Criteria Rubric</h2>
                <p className="text-xs text-slate-400 mt-0.5">
                  Total Criteria Weight: <strong className={Math.abs(totalCriteriaWeight - 1.0) < 0.001 ? 'text-emerald-400' : 'text-amber-400'}>
                    {totalCriteriaWeight.toFixed(2)} / 1.00
                  </strong>
                </p>
              </div>

              <button
                type="button"
                onClick={addCriterionRow}
                className="flex items-center gap-1 text-xs font-semibold text-cyan-400 hover:text-cyan-300 bg-cyan-500/10 border border-cyan-500/20 px-3 py-1.5 rounded-lg transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Criterion</span>
              </button>
            </div>

            <div className="space-y-3">
              {criteria.map((crit, idx) => (
                <div key={idx} className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl space-y-3 relative">
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    <input
                      type="text"
                      value={crit.name}
                      onChange={(e) => updateCriterionField(idx, 'name', e.target.value)}
                      placeholder="Criterion Name (e.g. System Design)"
                      className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-100 placeholder-slate-600 focus:outline-none focus:border-cyan-500/60"
                    />

                    <div className="flex items-center gap-2">
                      <span className="text-xs text-slate-400 whitespace-nowrap">Weight:</span>
                      <input
                        type="number"
                        step="0.05"
                        min="0.05"
                        max="1.0"
                        value={crit.weight}
                        onChange={(e) => updateCriterionField(idx, 'weight', e.target.value)}
                        className="w-full px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-100 focus:outline-none focus:border-cyan-500/60"
                      />
                    </div>

                    <select
                      value={crit.skillId}
                      onChange={(e) => updateCriterionField(idx, 'skillId', e.target.value)}
                      className="px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-200 focus:outline-none cursor-pointer"
                    >
                      <option value="">-- Associate Skill (Optional) --</option>
                      {availableSkills.map((s) => (
                        <option key={s.id} value={s.id}>{s.name}</option>
                      ))}
                    </select>
                  </div>

                  <input
                    type="text"
                    value={crit.description}
                    onChange={(e) => updateCriterionField(idx, 'description', e.target.value)}
                    placeholder="Criterion Description / Evaluation Guidelines..."
                    className="w-full px-3 py-1.5 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-100 placeholder-slate-600 focus:outline-none focus:border-cyan-500/60"
                  />

                  {criteria.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeCriterionRow(idx)}
                      className="absolute top-3 right-3 text-slate-500 hover:text-rose-400 p-1 transition-colors cursor-pointer"
                      title="Delete criterion"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
            <button
              type="button"
              disabled={submitting}
              onClick={() => handleSubmit(false)}
              className="flex items-center gap-2 px-5 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs rounded-xl transition-all cursor-pointer disabled:opacity-50"
            >
              <Save className="w-4 h-4" />
              <span>Save as Draft</span>
            </button>

            <button
              type="button"
              disabled={submitting}
              onClick={() => handleSubmit(true)}
              className="flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-400 hover:to-cyan-400 text-slate-950 font-bold text-xs rounded-xl shadow-lg shadow-emerald-500/20 transition-all cursor-pointer disabled:opacity-50"
            >
              {submitting ? <Loader2 className="w-4 h-4 animate-spin text-slate-950" /> : <Rocket className="w-4 h-4" />}
              <span>Publish Job</span>
            </button>
          </div>
        </div>
      </div>
    </RecruiterShell>
  );
};

export default CreateJobPage;
