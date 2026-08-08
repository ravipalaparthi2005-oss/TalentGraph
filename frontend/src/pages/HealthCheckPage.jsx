import React, { useState, useEffect } from 'react';
import { checkHealth } from '../services/api';
import { Activity, CheckCircle2, XCircle, RefreshCw, Server, ShieldCheck, Database, Cpu } from 'lucide-react';
import { motion } from 'framer-motion';

export default function HealthCheckPage() {
  const [healthData, setHealthData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastChecked, setLastChecked] = useState(null);

  const fetchHealth = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await checkHealth();
      setHealthData(res);
      setLastChecked(new Date().toLocaleTimeString());
    } catch (err) {
      setError(err.message || 'Failed to connect to backend service');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  return (
    <div className="max-w-4xl mx-auto px-6 py-12">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <div className="flex items-center justify-between border-b border-slate-800 pb-6 mb-8">
          <div>
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-cyan-500/10 border border-cyan-500/20 text-cyan-400">
                <Activity className="w-6 h-6" />
              </div>
              <h1 className="text-2xl font-bold text-slate-50 tracking-tight">System Foundation & Health</h1>
            </div>
            <p className="text-sm text-slate-400 mt-1">
              Phase 01 Monorepo Verification — Real backend connectivity check
            </p>
          </div>
          <button
            onClick={fetchHealth}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-200 rounded-lg transition-colors text-sm font-medium disabled:opacity-50 cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Re-test Connectivity
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          {/* Card 1: Frontend Status */}
          <div className="p-6 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <Cpu className="w-5 h-5 text-purple-400" />
                <h3 className="font-semibold text-slate-200">Frontend Client</h3>
              </div>
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                <CheckCircle2 className="w-3.5 h-3.5" /> Active (Vite + React)
              </span>
            </div>
            <div className="space-y-2 text-xs font-mono text-slate-400">
              <div className="flex justify-between py-1 border-b border-slate-800/50">
                <span>Runtime:</span>
                <span className="text-slate-200">JavaScript ES2022+ (No TS)</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-800/50">
                <span>Styling Engine:</span>
                <span className="text-slate-200">Tailwind CSS v4</span>
              </div>
              <div className="flex justify-between py-1">
                <span>Animation Stack:</span>
                <span className="text-slate-200">Framer Motion + GSAP</span>
              </div>
            </div>
          </div>

          {/* Card 2: Backend Status */}
          <div className="p-6 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <Server className="w-5 h-5 text-cyan-400" />
                <h3 className="font-semibold text-slate-200">Backend Service</h3>
              </div>
              {loading ? (
                <span className="text-xs text-slate-400 animate-pulse">Checking...</span>
              ) : healthData?.success ? (
                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  <CheckCircle2 className="w-3.5 h-3.5" /> Healthy (200 OK)
                </span>
              ) : (
                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                  <XCircle className="w-3.5 h-3.5" /> Disconnected
                </span>
              )}
            </div>
            <div className="space-y-2 text-xs font-mono text-slate-400">
              <div className="flex justify-between py-1 border-b border-slate-800/50">
                <span>Target Endpoint:</span>
                <span className="text-slate-200">/api/v1/health</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-800/50">
                <span>Backend Framework:</span>
                <span className="text-slate-200">Spring Boot 3 (Java 17)</span>
              </div>
              <div className="flex justify-between py-1">
                <span>Last Verified:</span>
                <span className="text-slate-200">{lastChecked || 'N/A'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Detailed Response Payload */}
        <div className="rounded-xl bg-slate-900 border border-slate-800 overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/40">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-cyan-400" />
              <h4 className="text-sm font-semibold text-slate-300">Backend Response Payload</h4>
            </div>
            <span className="text-xs text-slate-500 font-mono">REST JSON API</span>
          </div>

          <div className="p-6">
            {loading ? (
              <div className="p-8 text-center text-slate-500 text-sm">Testing REST connection...</div>
            ) : error ? (
              <div className="p-4 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm">
                <div className="font-semibold mb-1">Connection Error</div>
                <p className="text-xs opacity-90">{error}</p>
                <p className="text-xs mt-2 text-slate-400">
                  Ensure the Spring Boot backend is running on port 8080 (`cd backend && mvn spring-boot:run`).
                </p>
              </div>
            ) : (
              <pre className="p-4 rounded-lg bg-slate-950 font-mono text-xs text-emerald-400 overflow-x-auto border border-slate-800/80">
                {JSON.stringify(healthData, null, 2)}
              </pre>
            )}
          </div>
        </div>
      </motion.div>
    </div>
  );
}
