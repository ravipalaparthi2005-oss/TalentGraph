import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { User, Mail, Phone, MapPin, Github, Linkedin, Globe, Upload, FileText, AlertCircle, ArrowLeft, CheckCircle } from 'lucide-react';
import candidateService from '../services/candidateService';
import documentService from '../services/documentService';
import applicationService from '../services/applicationService';
import jobService from '../services/jobService';

export default function CreateCandidatePage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    location: '',
    linkedinUrl: '',
    githubUsername: '',
    portfolioUrl: '',
    jobId: '',
  });

  const [resumeFile, setResumeFile] = useState(null);
  const [jobs, setJobs] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchOpenJobs = async () => {
      try {
        const res = await jobService.getJobs({ status: 'OPEN' });
        if (res.success && res.data) {
          setJobs(res.data.content || []);
        }
      } catch (err) {
        console.error('Failed to fetch open jobs for candidate linking', err);
      }
    };
    fetchOpenJobs();
  }, []);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const validTypes = [
        'application/pdf',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      ];
      const validExts = ['.pdf', '.docx'];
      const fileExt = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();

      if (!validTypes.includes(file.type) && !validExts.includes(fileExt)) {
        setError('Unsupported document format. Please upload a PDF (.pdf) or Word (.docx) file.');
        setResumeFile(null);
        return;
      }

      if (file.size > 10 * 1024 * 1024) {
        setError('File size exceeds maximum limit of 10 MB.');
        setResumeFile(null);
        return;
      }

      setError(null);
      setResumeFile(file);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      // 1. Create candidate record in PostgreSQL
      const candRes = await candidateService.createCandidate({
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        phone: formData.phone || null,
        location: formData.location || null,
        linkedinUrl: formData.linkedinUrl || null,
        githubUsername: formData.githubUsername || null,
        portfolioUrl: formData.portfolioUrl || null,
      });

      const newCandidate = candRes.data;

      // 2. Link job application if job selected
      let appId = null;
      if (formData.jobId) {
        const appRes = await applicationService.createApplication(formData.jobId, {
          candidateId: newCandidate.id,
          source: 'RECRUITER',
        });
        appId = appRes.data?.id;
      }

      // 3. Upload resume document if attached
      if (resumeFile) {
        await documentService.uploadDocument(newCandidate.id, resumeFile, 'RESUME', appId);
      }

      navigate(`/app/candidates/${newCandidate.id}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create candidate record');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4 border-b border-slate-800 pb-5">
        <Link
          to="/app/candidates"
          className="p-2 text-slate-400 hover:text-white bg-slate-800/80 hover:bg-slate-800 border border-slate-700 rounded-lg transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-slate-100 tracking-tight">Intake New Candidate</h1>
          <p className="text-sm text-slate-400 mt-0.5">
            Add applicant details and upload PDF/DOCX resume for deterministic text extraction and evidence mapping
          </p>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-500/10 border border-red-500/20 text-red-400 rounded-xl text-sm flex items-center gap-3">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Personal Details */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 space-y-4">
          <h2 className="text-base font-semibold text-slate-200 border-b border-slate-800 pb-3 flex items-center gap-2">
            <User className="w-4 h-4 text-blue-400" />
            Candidate Identity
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">First Name *</label>
              <input
                type="text"
                name="firstName"
                required
                value={formData.firstName}
                onChange={handleChange}
                placeholder="e.g. Jane"
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500/50"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Last Name *</label>
              <input
                type="text"
                name="lastName"
                required
                value={formData.lastName}
                onChange={handleChange}
                placeholder="e.g. Doe"
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500/50"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Email Address *</label>
              <div className="relative">
                <Mail className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  type="email"
                  name="email"
                  required
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="jane.doe@example.com"
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500/50"
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Phone Number</label>
              <div className="relative">
                <Phone className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  type="tel"
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  placeholder="+1 (555) 019-2834"
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500/50"
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">Location</label>
            <div className="relative">
              <MapPin className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                name="location"
                value={formData.location}
                onChange={handleChange}
                placeholder="San Francisco, CA"
                className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500/50"
              />
            </div>
          </div>
        </div>

        {/* Public Profiles & Web Evidence */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 space-y-4">
          <h2 className="text-base font-semibold text-slate-200 border-b border-slate-800 pb-3 flex items-center gap-2">
            <Globe className="w-4 h-4 text-emerald-400" />
            Public Profiles & Evidence Accounts
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">GitHub Username</label>
              <div className="relative">
                <Github className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  type="text"
                  name="githubUsername"
                  value={formData.githubUsername}
                  onChange={handleChange}
                  placeholder="e.g. janedoe"
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500/50"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">LinkedIn URL</label>
              <div className="relative">
                <Linkedin className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  type="url"
                  name="linkedinUrl"
                  value={formData.linkedinUrl}
                  onChange={handleChange}
                  placeholder="https://linkedin.com/in/..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500/50"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Portfolio URL</label>
              <div className="relative">
                <Globe className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  type="url"
                  name="portfolioUrl"
                  value={formData.portfolioUrl}
                  onChange={handleChange}
                  placeholder="https://janedoe.dev"
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500/50"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Application Linkage & Resume File */}
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 space-y-4">
          <h2 className="text-base font-semibold text-slate-200 border-b border-slate-800 pb-3 flex items-center gap-2">
            <FileText className="w-4 h-4 text-purple-400" />
            Resume Upload & Job Application
          </h2>

          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">Apply to Open Opportunity (Optional)</label>
            <select
              name="jobId"
              value={formData.jobId}
              onChange={handleChange}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3.5 py-2 text-sm text-slate-200 focus:outline-none focus:border-blue-500/50"
            >
              <option value="">-- No initial job application --</option>
              {jobs.map((job) => (
                <option key={job.id} value={job.id}>
                  {job.title} ({job.department || 'General'})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">Resume Document (PDF or DOCX, max 10MB)</label>
            <div className="border-2 border-dashed border-slate-800 hover:border-slate-700 bg-slate-950 rounded-xl p-6 text-center transition-colors">
              <input
                type="file"
                id="resumeUpload"
                accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                onChange={handleFileChange}
                className="hidden"
              />
              <label htmlFor="resumeUpload" className="cursor-pointer space-y-2 block">
                <Upload className="w-8 h-8 mx-auto text-slate-500 hover:text-slate-300 transition-colors" />
                {resumeFile ? (
                  <div className="flex items-center justify-center gap-2 text-sm font-medium text-emerald-400">
                    <CheckCircle className="w-4 h-4" />
                    <span>{resumeFile.name} ({(resumeFile.size / 1024 / 1024).toFixed(2)} MB)</span>
                  </div>
                ) : (
                  <div>
                    <span className="text-sm font-medium text-blue-400">Click to select PDF or DOCX resume</span>
                    <p className="text-xs text-slate-500 mt-1">Raw text will be extracted deterministically using Apache PDFBox/POI</p>
                  </div>
                )}
              </label>
            </div>
          </div>
        </div>

        {/* Submit */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <Link
            to="/app/candidates"
            className="px-4 py-2 text-sm font-medium text-slate-400 hover:text-white bg-slate-800/80 hover:bg-slate-800 border border-slate-700 rounded-lg transition-colors"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={submitting}
            className="px-6 py-2 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white font-medium text-sm rounded-lg shadow-lg shadow-blue-500/10 transition-colors inline-flex items-center gap-2"
          >
            {submitting ? 'Ingesting Candidate & Evidence...' : 'Save & Intake Candidate'}
          </button>
        </div>
      </form>
    </div>
  );
}
