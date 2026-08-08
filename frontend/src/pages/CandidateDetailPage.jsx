import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { User, Mail, Phone, MapPin, Github, Linkedin, Globe, FileText, Download, CheckCircle, AlertTriangle, Briefcase, Plus, RefreshCw, ArrowLeft, ChevronDown, ChevronUp } from 'lucide-react';
import candidateService from '../services/candidateService';
import documentService from '../services/documentService';
import applicationService from '../services/applicationService';
import jobService from '../services/jobService';

export default function CandidateDetailPage() {
  const { candidateId } = useParams();
  const [candidate, setCandidate] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [applications, setApplications] = useState([]);
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [showRawTextId, setShowRawTextId] = useState(null);
  const [selectedJobId, setSelectedJobId] = useState('');
  const [applyingJob, setApplyingJob] = useState(false);

  const fetchCandidateDetails = async () => {
    setLoading(true);
    setError(null);
    try {
      const [candRes, docsRes, appsRes, jobsRes] = await Promise.all([
        candidateService.getCandidateById(candidateId),
        documentService.getCandidateDocuments(candidateId),
        applicationService.getCandidateApplications(candidateId),
        jobService.getJobs({ status: 'OPEN' }),
      ]);

      if (candRes.success) setCandidate(candRes.data);
      if (docsRes.success) setDocuments(docsRes.data || []);
      if (appsRes.success) setApplications(appsRes.data || []);
      if (jobsRes.success) setJobs(jobsRes.data.content || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load candidate profile');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCandidateDetails();
  }, [candidateId]);

  const handleFileUpload = async (e) => {
    if (!e.target.files || !e.target.files[0]) return;
    const file = e.target.files[0];
    setUploading(true);
    setError(null);
    try {
      await documentService.uploadDocument(candidateId, file, 'RESUME');
      await fetchCandidateDetails();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to upload and parse resume document');
    } finally {
      setUploading(false);
    }
  };

  const handleStatusChange = async (applicationId, newStatus) => {
    try {
      await applicationService.updateApplicationStatus(applicationId, newStatus);
      const appsRes = await applicationService.getCandidateApplications(candidateId);
      if (appsRes.success) setApplications(appsRes.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update application status');
    }
  };

  const handleApplyToJob = async () => {
    if (!selectedJobId) return;
    setApplyingJob(true);
    setError(null);
    try {
      await applicationService.createApplication(selectedJobId, { candidateId, source: 'RECRUITER' });
      setSelectedJobId('');
      const appsRes = await applicationService.getCandidateApplications(candidateId);
      if (appsRes.success) setApplications(appsRes.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to link job application');
    } finally {
      setApplyingJob(false);
    }
  };

  if (loading) {
    return (
      <div className="py-16 text-center text-slate-500 space-y-3">
        <RefreshCw className="w-8 h-8 animate-spin mx-auto text-blue-500" />
        <p className="text-sm">Loading candidate profile and evidence graph...</p>
      </div>
    );
  }

  if (error || !candidate) {
    return (
      <div className="max-w-3xl mx-auto py-12 space-y-4">
        <Link to="/app/candidates" className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white">
          <ArrowLeft className="w-4 h-4" /> Back to Candidate Directory
        </Link>
        <div className="p-4 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl text-sm">
          {error || 'Candidate profile not found.'}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Navigation Header */}
      <div className="flex items-center justify-between border-b border-slate-800 pb-4">
        <Link to="/app/candidates" className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors">
          <ArrowLeft className="w-4 h-4" /> Candidate Roster
        </Link>
        <button
          onClick={fetchCandidateDetails}
          className="p-2 text-slate-400 hover:text-white bg-slate-800/80 hover:bg-slate-800 border border-slate-700 rounded-lg transition-colors text-xs inline-flex items-center gap-1.5"
        >
          <RefreshCw className="w-3.5 h-3.5" /> Refresh Profile
        </button>
      </div>

      {/* Candidate Profile Header Card */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-6 shadow-xl space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-slate-100">{candidate.firstName} {candidate.lastName}</h1>
              <span className="px-2.5 py-0.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs font-mono rounded-full">
                Active Applicant
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-4 text-xs text-slate-400 font-mono pt-1">
              <span className="flex items-center gap-1.5"><Mail className="w-3.5 h-3.5 text-slate-500" /> {candidate.email}</span>
              {candidate.phone && <span className="flex items-center gap-1.5"><Phone className="w-3.5 h-3.5 text-slate-500" /> {candidate.phone}</span>}
              {candidate.location && <span className="flex items-center gap-1.5"><MapPin className="w-3.5 h-3.5 text-slate-500" /> {candidate.location}</span>}
            </div>
          </div>

          <div className="flex items-center gap-3">
            {candidate.githubUsername && (
              <a
                href={`https://github.com/${candidate.githubUsername}`}
                target="_blank"
                rel="noreferrer"
                className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium rounded-lg border border-slate-700 inline-flex items-center gap-1.5 transition-colors"
              >
                <Github className="w-3.5 h-3.5" /> GitHub
              </a>
            )}
            {candidate.linkedinUrl && (
              <a
                href={candidate.linkedinUrl}
                target="_blank"
                rel="noreferrer"
                className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium rounded-lg border border-slate-700 inline-flex items-center gap-1.5 transition-colors"
              >
                <Linkedin className="w-3.5 h-3.5 text-blue-400" /> LinkedIn
              </a>
            )}
            {candidate.portfolioUrl && (
              <a
                href={candidate.portfolioUrl}
                target="_blank"
                rel="noreferrer"
                className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium rounded-lg border border-slate-700 inline-flex items-center gap-1.5 transition-colors"
              >
                <Globe className="w-3.5 h-3.5 text-emerald-400" /> Portfolio
              </a>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Applications & Ingestion */}
        <div className="lg:col-span-2 space-y-6">
          {/* Active Job Applications */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h2 className="text-base font-semibold text-slate-200 flex items-center gap-2">
                <Briefcase className="w-4 h-4 text-blue-400" /> Job Applications ({applications.length})
              </h2>
            </div>

            {/* Apply to New Job Dropdown */}
            <div className="flex items-center gap-3 bg-slate-950 p-3 rounded-lg border border-slate-800">
              <select
                value={selectedJobId}
                onChange={(e) => setSelectedJobId(e.target.value)}
                className="w-full bg-transparent text-xs text-slate-200 focus:outline-none"
              >
                <option value="" className="bg-slate-900">-- Link to Open Job Opportunity --</option>
                {jobs.map((j) => (
                  <option key={j.id} value={j.id} className="bg-slate-900">
                    {j.title} ({j.department || 'General'})
                  </option>
                ))}
              </select>
              <button
                disabled={!selectedJobId || applyingJob}
                onClick={handleApplyToJob}
                className="px-3 py-1.5 bg-blue-600 hover:bg-blue-500 disabled:opacity-40 text-white text-xs font-medium rounded-lg transition-colors whitespace-nowrap inline-flex items-center gap-1"
              >
                <Plus className="w-3.5 h-3.5" /> Link Application
              </button>
            </div>

            {applications.length === 0 ? (
              <p className="text-xs text-slate-500 py-4 text-center">Candidate is not currently attached to any active job opportunities.</p>
            ) : (
              <div className="divide-y divide-slate-800/60">
                {applications.map((app) => (
                  <div key={app.id} className="py-3 flex items-center justify-between">
                    <div>
                      <Link to={`/app/jobs/${app.jobId}`} className="text-sm font-medium text-slate-200 hover:text-blue-400 transition-colors">
                        {app.jobTitle}
                      </Link>
                      <div className="text-xs text-slate-500 font-mono mt-0.5">Applied: {new Date(app.appliedAt).toLocaleDateString()}</div>
                    </div>
                    <div className="flex items-center gap-3">
                      <select
                        value={app.status}
                        onChange={(e) => handleStatusChange(app.id, e.target.value)}
                        className="bg-slate-950 border border-slate-700 text-xs font-mono text-slate-300 rounded-lg px-2.5 py-1 focus:outline-none"
                      >
                        {['NEW', 'SCREENING', 'ASSESSMENT', 'INTERVIEW', 'OFFER', 'HIRED', 'REJECTED', 'WITHDRAWN'].map((st) => (
                          <option key={st} value={st}>{st}</option>
                        ))}
                      </select>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Uploaded Resume Documents */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h2 className="text-base font-semibold text-slate-200 flex items-center gap-2">
                <FileText className="w-4 h-4 text-purple-400" /> Uploaded Resume Documents ({documents.length})
              </h2>
              <label className="cursor-pointer px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium rounded-lg border border-slate-700 transition-colors inline-flex items-center gap-1.5">
                <Plus className="w-3.5 h-3.5" /> Upload Resume
                <input
                  type="file"
                  accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                  onChange={handleFileUpload}
                  className="hidden"
                />
              </label>
            </div>

            {uploading && (
              <div className="p-3 bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs rounded-lg flex items-center gap-2">
                <RefreshCw className="w-4 h-4 animate-spin" /> Uploading, hashing, and extracting text with PDFBox/POI...
              </div>
            )}

            {documents.length === 0 ? (
              <p className="text-xs text-slate-500 py-6 text-center">No resume documents uploaded yet.</p>
            ) : (
              <div className="space-y-3">
                {documents.map((doc) => (
                  <div key={doc.id} className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <FileText className="w-5 h-5 text-purple-400 flex-shrink-0" />
                        <div>
                          <h3 className="text-sm font-medium text-slate-200">{doc.originalFilename}</h3>
                          <div className="text-xs text-slate-500 font-mono mt-0.5">
                            {(doc.fileSizeBytes / 1024).toFixed(1)} KB • SHA-256: {doc.sha256Hash?.substring(0, 16)}...
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className={`px-2.5 py-0.5 text-xs font-mono rounded-full border ${
                          doc.processingStatus === 'PROCESSED'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : 'bg-red-500/10 text-red-400 border-red-500/20'
                        }`}>
                          {doc.processingStatus}
                        </span>
                        <button
                          onClick={() => documentService.downloadDocument(candidateId, doc.id, doc.originalFilename)}
                          className="p-1.5 text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg transition-colors"
                          title="Download stored document"
                        >
                          <Download className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>

                    {/* Raw Text Accordion */}
                    {doc.rawText && (
                      <div className="pt-2 border-t border-slate-800/80">
                        <button
                          onClick={() => setShowRawTextId(showRawTextId === doc.id ? null : doc.id)}
                          className="text-xs text-slate-400 hover:text-slate-200 font-medium inline-flex items-center gap-1"
                        >
                          {showRawTextId === doc.id ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                          {showRawTextId === doc.id ? 'Hide Extracted Resume Text' : 'View Extracted Resume Text'}
                        </button>

                        {showRawTextId === doc.id && (
                          <div className="mt-2 p-3 bg-slate-900 border border-slate-800 rounded-lg text-xs font-mono text-slate-300 max-h-60 overflow-y-auto whitespace-pre-wrap">
                            {doc.rawText}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Evidence Summary */}
        <div className="space-y-6">
          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-4">
            <h2 className="text-base font-semibold text-slate-200 border-b border-slate-800 pb-3 flex items-center gap-2">
              <CheckCircle className="w-4 h-4 text-emerald-400" /> Evidence Graph Intake
            </h2>
            <p className="text-xs text-slate-400">
              Deterministic skill mentions extracted directly from uploaded resume documents. Zero hallucinated skills or synthetic scores.
            </p>

            <div className="p-3 bg-slate-950 border border-slate-800 rounded-lg space-y-2">
              <div className="text-xs font-medium text-slate-300 flex justify-between">
                <span>Ingestion Mode</span>
                <span className="text-emerald-400 font-mono">DETERMINISTIC</span>
              </div>
              <div className="text-xs font-medium text-slate-300 flex justify-between">
                <span>LLM Extraction</span>
                <span className="text-slate-500 font-mono">DISABLED (PHASE 05)</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
