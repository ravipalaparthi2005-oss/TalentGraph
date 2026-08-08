import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import RecruiterShell from '../components/RecruiterShell';
import { useAuth } from '../context/AuthContext';
import {
  getJobById,
  updateJob,
  getJobRequirements,
  addJobRequirement,
  deleteJobRequirement,
  getJobCriteria,
  addJobCriterion,
  deleteJobCriterion
} from '../services/jobService';
import { getSkills } from '../services/skillService';
import { ArrowLeft, Save, Trash2, Plus, AlertCircle, Loader2 } from 'lucide-react';

const EditJobPage = () => {
  const { jobId } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const [title, setTitle] = useState('');
  const [department, setDepartment] = useState('');
  const [location, setLocation] = useState('');
  const [employmentType, setEmploymentType] = useState('FULL_TIME');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState('DRAFT');

  const [existingRequirements, setExistingRequirements] = useState([]);
  const [newRequirements, setNewRequirements] = useState([]);

  const [existingCriteria, setExistingCriteria] = useState([]);
  const [newCriteria, setNewCriteria] = useState([]);

  const [availableSkills, setAvailableSkills] = useState([]);

  useEffect(() => {
    fetchInitialData();
  }, [jobId]);

  const fetchInitialData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [jobRes, reqsRes, critRes, skillsRes] = await Promise.all([
        getJobById(jobId),
        getJobRequirements(jobId),
        getJobCriteria(jobId),
        getSkills()
      ]);

      if (jobRes?.data) {
        const j = jobRes.data;
        setTitle(j.title);
        setDepartment(j.department || '');
        setLocation(j.location || '');
        setEmploymentType(j.employmentType);
        setDescription(j.description);
        setStatus(j.status);
      }

      if (reqsRes?.data) setExistingRequirements(reqsRes.data);
      if (critRes?.data) setExistingCriteria(critRes.data);
      if (skillsRes?.data) setAvailableSkills(skillsRes.data);
    } catch (err) {
      setError(err.message || 'Failed to load job details for editing.');
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveExistingReq = async (reqId) => {
    try {
      await deleteJobRequirement(jobId, reqId);
      setExistingRequirements((prev) => prev.filter((r) => r.id !== reqId));
    } catch (err) {
      setError(err.message || 'Failed to delete requirement.');
    }
  };

  const handleRemoveExistingCrit = async (critId) => {
    try {
      await deleteJobCriterion(jobId, critId);
      setExistingCriteria((prev) => prev.filter((c) => c.id !== critId));
    } catch (err) {
      setError(err.message || 'Failed to delete criterion.');
    }
  };

  const handleSave = async () => {
    if (!title.trim() || !description.trim()) {
      setError('Title and Description are required.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await updateJob(jobId, {
        title: title.trim(),
        department: department.trim(),
        location: location.trim(),
        employmentType,
        description: description.trim()
      });

      for (const req of newRequirements) {
        if (req.name.trim()) {
          await addJobRequirement(jobId, {
            name: req.name.trim(),
            description: req.description.trim(),
            requirementType: req.requirementType,
            importance: req.importance,
            minimumLevel: req.minimumLevel,
            skillId: req.skillId || null
          });
        }
      }

      for (const crit of newCriteria) {
        if (crit.name.trim()) {
          await addJobCriterion(jobId, {
            name: crit.name.trim(),
            description: crit.description.trim(),
            weight: parseFloat(crit.weight) || 0.1,
            skillId: crit.skillId || null
          });
        }
      }

      navigate(`/app/jobs/${jobId}`);
    } catch (err) {
      setError(err.message || 'Failed to save job updates.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <RecruiterShell>
        <div className="py-20 flex flex-col items-center justify-center text-slate-400">
          <Loader2 className="w-8 h-8 animate-spin text-emerald-500 mb-3" />
          <p className="text-xs font-medium tracking-wide">Loading job configuration...</p>
        </div>
      </RecruiterShell>
    );
  }

  return (
    <RecruiterShell>
      <div className="max-w-4xl mx-auto space-y-6 pb-12">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link to={`/app/jobs/${jobId}`} className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors">
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <div>
              <h1 className="text-xl font-bold text-white tracking-tight">Edit Job: {title}</h1>
              <p className="text-xs text-slate-400">Status: <span className="text-amber-400 font-semibold">{status}</span></p>
            </div>
          </div>
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-3">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <div className="space-y-6">
          {/* Job Info */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-4 backdrop-blur-sm">
            <h2 className="text-xs font-bold text-emerald-400 uppercase tracking-wider">Basic Information</h2>

            <div>
              <label htmlFor="edit-title" className="block text-xs font-medium text-slate-300 mb-1">Job Title</label>
              <input
                id="edit-title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full px-3.5 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-emerald-500/80"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label htmlFor="edit-department" className="block text-xs font-medium text-slate-300 mb-1">Department</label>
                <input
                  id="edit-department"
                  type="text"
                  value={department}
                  onChange={(e) => setDepartment(e.target.value)}
                  className="w-full px-3.5 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-emerald-500/80"
                />
              </div>

              <div>
                <label htmlFor="edit-location" className="block text-xs font-medium text-slate-300 mb-1">Location</label>
                <input
                  id="edit-location"
                  type="text"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  className="w-full px-3.5 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-emerald-500/80"
                />
              </div>

              <div>
                <label htmlFor="edit-employmentType" className="block text-xs font-medium text-slate-300 mb-1">Employment Type</label>
                <select
                  id="edit-employmentType"
                  value={employmentType}
                  onChange={(e) => setEmploymentType(e.target.value)}
                  className="w-full px-3.5 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-emerald-500/80 cursor-pointer"
                >
                  <option value="FULL_TIME">Full Time</option>
                  <option value="PART_TIME">Part Time</option>
                  <option value="CONTRACT">Contract</option>
                  <option value="INTERNSHIP">Internship</option>
                </select>
              </div>
            </div>

            <div>
              <label htmlFor="edit-description" className="block text-xs font-medium text-slate-300 mb-1">Job Description</label>
              <textarea
                id="edit-description"
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full px-3.5 py-2 bg-slate-950/80 border border-slate-800 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-emerald-500/80"
              />
            </div>
          </div>

          {/* Requirements Management */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-4 backdrop-blur-sm">
            <h2 className="text-xs font-bold text-emerald-400 uppercase tracking-wider">Existing Requirements</h2>
            <div className="space-y-2">
              {existingRequirements.map((r) => (
                <div key={r.id} className="p-3 bg-slate-950/80 border border-slate-800 rounded-xl flex items-center justify-between text-xs">
                  <div>
                    <span className="font-semibold text-slate-100">{r.name}</span>
                    <span className="text-slate-500 ml-2 font-mono text-[10px]">({r.requirementType})</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleRemoveExistingReq(r.id)}
                    className="text-slate-500 hover:text-rose-400 p-1 cursor-pointer"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
            <button
              type="button"
              disabled={submitting}
              onClick={handleSave}
              className="flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-400 hover:to-cyan-400 text-slate-950 font-bold text-xs rounded-xl shadow-lg shadow-emerald-500/20 transition-all cursor-pointer disabled:opacity-50"
            >
              {submitting ? <Loader2 className="w-4 h-4 animate-spin text-slate-950" /> : <Save className="w-4 h-4" />}
              <span>Save Changes</span>
            </button>
          </div>
        </div>
      </div>
    </RecruiterShell>
  );
};

export default EditJobPage;
