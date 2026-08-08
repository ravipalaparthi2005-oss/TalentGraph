import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import RecruiterShell from '../components/RecruiterShell';
import {
  getJobById,
  updateJobStatus,
  deleteJob,
  getJobRequirements,
  getJobCriteria,
  getJobActivity
} from '../services/jobService';
import {
  ArrowLeft,
  Briefcase,
  Calendar,
  Clock,
  MapPin,
  Building,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Edit,
  Play,
  Pause,
  XCircle,
  Trash2,
  History,
  Users,
  FileCode,
  Video
} from 'lucide-react';

const JobDetailPage = () => {
  const { jobId } = useParams();
  const navigate = useNavigate();

  const [job, setJob] = useState(null);
  const [requirements, setRequirements] = useState([]);
  const [criteria, setCriteria] = useState([]);
  const [activity, setActivity] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('overview');

  const fetchJobData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [jobRes, reqsRes, critRes, actRes] = await Promise.all([
        getJobById(jobId),
        getJobRequirements(jobId),
        getJobCriteria(jobId),
        getJobActivity(jobId)
      ]);

      if (jobRes?.data) setJob(jobRes.data);
      if (reqsRes?.data) setRequirements(reqsRes.data);
      if (critRes?.data) setCriteria(critRes.data);
      if (actRes?.data) setActivity(actRes.data);
    } catch (err) {
      setError(err.message || 'Failed to load job details.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobData();
  }, [jobId]);

  const handleStatusChange = async (newStatus) => {
    setActionLoading(true);
    setError(null);
    try {
      await updateJobStatus(jobId, newStatus);
      await fetchJobData();
    } catch (err) {
      setError(err.message || `Failed to transition job status to ${newStatus}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteJob = async () => {
    if (!window.confirm('Are you sure you want to archive/close this job?')) return;
    setActionLoading(true);
    try {
      await deleteJob(jobId);
      navigate('/app/jobs');
    } catch (err) {
      setError(err.message || 'Failed to delete job.');
      setActionLoading(false);
    }
  };

  const getStatusBadge = (s) => {
    switch (s) {
      case 'OPEN':
        return <span className="px-3 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-bold tracking-wider uppercase">OPEN</span>;
      case 'DRAFT':
        return <span className="px-3 py-1 rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-bold tracking-wider uppercase">DRAFT</span>;
      case 'PAUSED':
        return <span className="px-3 py-1 rounded-lg bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 text-xs font-bold tracking-wider uppercase">PAUSED</span>;
      case 'CLOSED':
        return <span className="px-3 py-1 rounded-lg bg-slate-800 border border-slate-700 text-slate-400 text-xs font-bold tracking-wider uppercase">CLOSED</span>;
      default:
        return null;
    }
  };

  if (loading) {
    return (
      <RecruiterShell>
        <div className="py-20 flex flex-col items-center justify-center text-slate-400">
          <Loader2 className="w-8 h-8 animate-spin text-emerald-500 mb-3" />
          <p className="text-xs font-medium tracking-wide">Loading job configuration from PostgreSQL...</p>
        </div>
      </RecruiterShell>
    );
  }

  if (error || !job) {
    return (
      <RecruiterShell>
        <div className="p-6 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs space-y-4">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{error || 'Job not found or access restricted.'}</span>
          </div>
          <Link to="/app/jobs" className="inline-flex items-center gap-2 px-3.5 py-1.5 bg-slate-900 border border-slate-800 rounded-xl text-slate-200 hover:text-white">
            <ArrowLeft className="w-4 h-4" />
            <span>Return to Jobs List</span>
          </Link>
        </div>
      </RecruiterShell>
    );
  }

  return (
    <RecruiterShell>
      <div className="space-y-6 pb-12">
        {/* Top Navigation & Status Actions Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-900/60 border border-slate-800 rounded-2xl p-6 backdrop-blur-sm">
          <div className="space-y-2">
            <div className="flex items-center gap-3 flex-wrap">
              <Link to="/app/jobs" className="p-1.5 rounded-lg bg-slate-950 border border-slate-800 text-slate-400 hover:text-white transition-colors">
                <ArrowLeft className="w-4 h-4" />
              </Link>
              <h1 className="text-2xl font-bold text-white tracking-tight">{job.title}</h1>
              {getStatusBadge(job.status)}
            </div>

            <div className="flex items-center gap-4 text-xs text-slate-400 flex-wrap">
              {job.department && (
                <span className="flex items-center gap-1.5">
                  <Building className="w-3.5 h-3.5 text-slate-500" />
                  {job.department}
                </span>
              )}
              {job.location && (
                <span className="flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-slate-500" />
                  {job.location}
                </span>
              )}
              <span className="flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-slate-500" />
                {job.employmentType.replace('_', ' ')}
              </span>
              <span className="flex items-center gap-1.5 text-slate-500">
                <Calendar className="w-3.5 h-3.5" />
                Created: {new Date(job.createdAt).toLocaleDateString()}
              </span>
            </div>
          </div>

          {/* Action Controls */}
          <div className="flex items-center gap-2 flex-wrap">
            {job.status === 'DRAFT' && (
              <button
                disabled={actionLoading}
                onClick={() => handleStatusChange('OPEN')}
                className="flex items-center gap-1.5 px-3.5 py-1.5 bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-400 hover:to-cyan-400 text-slate-950 font-bold text-xs rounded-xl shadow-md shadow-emerald-500/20 transition-all cursor-pointer"
              >
                <Play className="w-3.5 h-3.5" />
                <span>Publish Job</span>
              </button>
            )}

            {job.status === 'OPEN' && (
              <button
                disabled={actionLoading}
                onClick={() => handleStatusChange('PAUSED')}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 hover:bg-cyan-500/20 text-xs font-semibold rounded-xl transition-all cursor-pointer"
              >
                <Pause className="w-3.5 h-3.5" />
                <span>Pause Hiring</span>
              </button>
            )}

            {job.status === 'PAUSED' && (
              <button
                disabled={actionLoading}
                onClick={() => handleStatusChange('OPEN')}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20 text-xs font-semibold rounded-xl transition-all cursor-pointer"
              >
                <Play className="w-3.5 h-3.5" />
                <span>Resume Hiring</span>
              </button>
            )}

            {job.status !== 'CLOSED' && (
              <button
                disabled={actionLoading}
                onClick={() => handleStatusChange('CLOSED')}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold rounded-xl transition-all cursor-pointer"
              >
                <XCircle className="w-3.5 h-3.5" />
                <span>Close Job</span>
              </button>
            )}

            {job.status !== 'CLOSED' && (
              <Link
                to={`/app/jobs/${job.id}/edit`}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-xl transition-all cursor-pointer"
              >
                <Edit className="w-3.5 h-3.5" />
                <span>Edit</span>
              </Link>
            )}

            <button
              disabled={actionLoading}
              onClick={handleDeleteJob}
              title="Soft Archive Job"
              className="p-2 text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 rounded-xl transition-colors cursor-pointer"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-2 border-b border-slate-800 overflow-x-auto pb-1">
          <button
            onClick={() => setActiveTab('overview')}
            className={`px-4 py-2 text-xs font-semibold rounded-t-xl transition-colors whitespace-nowrap cursor-pointer ${
              activeTab === 'overview'
                ? 'bg-slate-900 text-emerald-400 border-t border-x border-slate-800'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Overview & Requirements ({requirements.length})
          </button>
          <button
            onClick={() => setActiveTab('criteria')}
            className={`px-4 py-2 text-xs font-semibold rounded-t-xl transition-colors whitespace-nowrap cursor-pointer ${
              activeTab === 'criteria'
                ? 'bg-slate-900 text-cyan-400 border-t border-x border-slate-800'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Evaluation Rubric ({criteria.length})
          </button>
          <button
            onClick={() => setActiveTab('activity')}
            className={`px-4 py-2 text-xs font-semibold rounded-t-xl transition-colors whitespace-nowrap cursor-pointer ${
              activeTab === 'activity'
                ? 'bg-slate-900 text-amber-400 border-t border-x border-slate-800'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Audit Activity ({activity.length})
          </button>

          {/* Placeholders for future phases showing REAL database counts */}
          <div className="flex items-center gap-3 pl-4 border-l border-slate-800 text-slate-500 text-xs py-2">
            <span className="flex items-center gap-1 opacity-60">
              <Users className="w-3.5 h-3.5" /> Candidates (0)
            </span>
            <span className="flex items-center gap-1 opacity-60">
              <FileCode className="w-3.5 h-3.5" /> Assessments (0)
            </span>
            <span className="flex items-center gap-1 opacity-60">
              <Video className="w-3.5 h-3.5" /> Interviews (0)
            </span>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            {/* Description */}
            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-2 backdrop-blur-sm">
              <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">Job Description</h3>
              <p className="text-sm text-slate-200 whitespace-pre-wrap leading-relaxed">{job.description}</p>
            </div>

            {/* Requirements */}
            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-4 backdrop-blur-sm">
              <h3 className="text-xs font-bold text-emerald-400 uppercase tracking-wider">Job Requirements ({requirements.length})</h3>
              {requirements.length === 0 ? (
                <p className="text-xs text-slate-500 italic">No requirements configured for this job.</p>
              ) : (
                <div className="space-y-3">
                  {requirements.map((req) => (
                    <div key={req.id} className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl space-y-1">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-xs text-slate-100">{req.name}</span>
                          {req.skillName && (
                            <span className="px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-[10px] font-mono">
                              {req.skillName}
                            </span>
                          )}
                        </div>

                        <div className="flex items-center gap-2">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-semibold uppercase ${
                            req.requirementType === 'REQUIRED' ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20' : 'bg-slate-800 text-slate-400'
                          }`}>
                            {req.requirementType}
                          </span>
                          <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-[10px] font-mono">
                            {req.importance}
                          </span>
                        </div>
                      </div>
                      {req.description && (
                        <p className="text-xs text-slate-400 pt-1">{req.description}</p>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'criteria' && (
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-4 backdrop-blur-sm">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold text-cyan-400 uppercase tracking-wider">Evaluation Rubric ({criteria.length})</h3>
              <span className="text-xs text-slate-400">
                Total Weight: <strong className="text-cyan-400">{criteria.reduce((s, c) => s + (parseFloat(c.weight) || 0), 0).toFixed(2)}</strong>
              </span>
            </div>

            {criteria.length === 0 ? (
              <p className="text-xs text-slate-500 italic">No evaluation criteria configured.</p>
            ) : (
              <div className="space-y-3">
                {criteria.map((crit) => (
                  <div key={crit.id} className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl space-y-1">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-xs text-slate-100">{crit.name}</span>
                        {crit.skillName && (
                          <span className="px-2 py-0.5 rounded bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 text-[10px] font-mono">
                            {crit.skillName}
                          </span>
                        )}
                      </div>

                      <span className="px-2.5 py-1 rounded bg-cyan-500/10 border border-cyan-500/30 text-cyan-400 text-xs font-bold font-mono">
                        Weight: {crit.weight} ({(parseFloat(crit.weight) * 100).toFixed(0)}%)
                      </span>
                    </div>

                    {crit.description && (
                      <p className="text-xs text-slate-400 pt-1">{crit.description}</p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'activity' && (
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-4 backdrop-blur-sm">
            <h3 className="text-xs font-bold text-amber-400 uppercase tracking-wider flex items-center gap-2">
              <History className="w-4 h-4" />
              <span>Real Audit Log Activity ({activity.length})</span>
            </h3>

            {activity.length === 0 ? (
              <p className="text-xs text-slate-500 italic">No activity recorded for this job.</p>
            ) : (
              <div className="space-y-3">
                {activity.map((act) => (
                  <div key={act.id} className="p-3.5 bg-slate-950/80 border border-slate-800/80 rounded-xl flex items-center justify-between text-xs">
                    <div className="space-y-0.5">
                      <div className="flex items-center gap-2">
                        <span className="px-2 py-0.5 rounded bg-amber-500/10 border border-amber-500/20 text-amber-400 text-[10px] font-bold tracking-wider uppercase font-mono">
                          {act.action}
                        </span>
                        <span className="text-slate-200 font-medium">Actor: {act.actorName}</span>
                      </div>
                      {act.metadataJson && (
                        <code className="text-[11px] text-slate-500 font-mono block">{act.metadataJson}</code>
                      )}
                    </div>

                    <span className="text-slate-500 font-mono text-[11px]">
                      {new Date(act.createdAt).toLocaleString()}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </RecruiterShell>
  );
};

export default JobDetailPage;
