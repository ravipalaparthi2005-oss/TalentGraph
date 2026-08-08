import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, Plus, Briefcase, LogOut, ChevronDown, User, Layers } from 'lucide-react';

const RecruiterShell = ({ children }) => {
  const { user, activeOrganization, setActiveOrganization, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const isJobsActive = location.pathname === '/app' || location.pathname.startsWith('/app/jobs');

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* Top Navbar */}
      <header className="border-b border-slate-800/80 bg-slate-900/80 backdrop-blur-xl sticky top-0 z-40 px-4 sm:px-8 py-3.5">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-6">
            <Link to="/app/jobs" className="flex items-center gap-3 group">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-500 to-cyan-500 p-0.5 shadow-lg shadow-emerald-500/20 group-hover:scale-105 transition-transform">
                <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center">
                  <ShieldCheck className="w-5 h-5 text-emerald-400" />
                </div>
              </div>
              <div>
                <span className="font-bold text-sm text-white tracking-tight">TalentGraph</span>
                <span className="block text-[10px] text-slate-400 font-mono">Recruiter Workspace</span>
              </div>
            </Link>

            {/* Navigation Links */}
            <nav className="hidden md:flex items-center gap-1 pl-4 border-l border-slate-800">
              <Link
                to="/app/jobs"
                className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  isJobsActive
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
                }`}
              >
                <Briefcase className="w-4 h-4" />
                <span>Jobs</span>
              </Link>
            </nav>
          </div>

          {/* Right Actions & Active Organization Selector */}
          <div className="flex items-center gap-3">
            {/* Active Organization Switcher */}
            {user?.memberships && user.memberships.length > 0 && (
              <div className="relative">
                <select
                  value={activeOrganization?.organizationId || ''}
                  onChange={(e) => {
                    const selected = user.memberships.find(m => m.organizationId === e.target.value);
                    if (selected) setActiveOrganization(selected);
                  }}
                  className="bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-medium rounded-xl px-3 py-1.5 pr-8 appearance-none focus:outline-none focus:ring-1 focus:ring-emerald-500/80 cursor-pointer"
                >
                  {user.memberships.map((m) => (
                    <option key={m.organizationId} value={m.organizationId} className="bg-slate-900 text-slate-200">
                      {m.organizationName} ({m.role})
                    </option>
                  ))}
                </select>
                <div className="absolute inset-y-0 right-0 flex items-center pr-2.5 pointer-events-none text-slate-400">
                  <ChevronDown className="w-3.5 h-3.5" />
                </div>
              </div>
            )}

            {/* Create Job CTA Button */}
            <Link
              to="/app/jobs/new"
              className="flex items-center gap-1.5 px-3.5 py-1.5 bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-400 hover:to-cyan-400 text-slate-950 font-semibold text-xs rounded-xl shadow-md shadow-emerald-500/20 transition-all cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>Create Job</span>
            </Link>

            {/* User Profile Badge */}
            <div className="hidden lg:flex items-center gap-2 px-2.5 py-1.5 bg-slate-900/80 border border-slate-800 rounded-xl text-xs text-slate-300">
              <User className="w-3.5 h-3.5 text-emerald-400" />
              <span className="font-medium">{user?.firstName}</span>
            </div>

            {/* Logout */}
            <button
              onClick={logout}
              title="Sign Out"
              className="p-2 text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 rounded-xl transition-colors cursor-pointer border border-transparent hover:border-rose-500/20"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* Main Page Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
        {children}
      </main>
    </div>
  );
};

export default RecruiterShell;
