import React from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, LogOut, Building2, User, Key, Layers } from 'lucide-react';

const DashboardShellPage = () => {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* Top Navigation */}
      <header className="border-b border-slate-800/80 bg-slate-900/60 backdrop-blur-xl px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-gradient-to-tr from-emerald-500 to-cyan-500 p-0.5 shadow-md shadow-emerald-500/20">
            <div className="w-full h-full bg-slate-950 rounded-[7px] flex items-center justify-center">
              <ShieldCheck className="w-5 h-5 text-emerald-400" />
            </div>
          </div>
          <div>
            <h1 className="font-bold text-sm text-white tracking-tight">TalentGraph</h1>
            <p className="text-[10px] text-slate-400">Authenticated Recruiter Shell</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 text-xs text-slate-300 bg-slate-900 border border-slate-800 px-3 py-1.5 rounded-xl">
            <User className="w-3.5 h-3.5 text-emerald-400" />
            <span>{user?.firstName} {user?.lastName}</span>
            <span className="text-slate-500">({user?.email})</span>
          </div>

          <button
            onClick={logout}
            className="flex items-center gap-1.5 text-xs font-medium text-slate-400 hover:text-rose-400 bg-slate-900 hover:bg-rose-500/10 border border-slate-800 hover:border-rose-500/30 px-3 py-1.5 rounded-xl transition-all cursor-pointer"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Sign Out</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-5xl w-full mx-auto p-6 sm:p-8 space-y-6">
        <div className="bg-slate-900/50 border border-slate-800/80 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
              <Key className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-white">Authenticated Session State</h2>
              <p className="text-xs text-slate-400">Validated via Spring Security 6 stateless JWT architecture</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <div className="bg-slate-950/60 p-4 rounded-xl border border-slate-800/80">
              <span className="text-slate-500 block mb-1">User Identifier (UUID)</span>
              <code className="text-emerald-400 font-mono text-[11px]">{user?.id}</code>
            </div>
            <div className="bg-slate-950/60 p-4 rounded-xl border border-slate-800/80">
              <span className="text-slate-500 block mb-1">Normalized Email</span>
              <span className="text-slate-200 font-medium">{user?.email}</span>
            </div>
          </div>
        </div>

        {/* Workspace Memberships */}
        <div className="bg-slate-900/50 border border-slate-800/80 rounded-2xl p-6 shadow-xl backdrop-blur-sm">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400">
              <Building2 className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-white">Multi-Tenant Organizations</h2>
              <p className="text-xs text-slate-400">Authorized workspaces and role permissions</p>
            </div>
          </div>

          {user?.memberships && user.memberships.length > 0 ? (
            <div className="space-y-3">
              {user.memberships.map((m) => (
                <div
                  key={m.organizationId}
                  className="flex items-center justify-between p-4 bg-slate-950/60 border border-slate-800/80 rounded-xl"
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-slate-900 rounded-lg text-slate-400">
                      <Layers className="w-4 h-4" />
                    </div>
                    <div>
                      <h3 className="font-semibold text-xs text-slate-200">{m.organizationName}</h3>
                      <p className="text-[11px] text-slate-500 font-mono">slug: {m.organizationSlug}</p>
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded-md bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-[11px] font-semibold uppercase tracking-wider">
                    {m.role}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-slate-500 italic">No organization memberships associated with this user.</p>
          )}
        </div>
      </main>
    </div>
  );
};

export default DashboardShellPage;
