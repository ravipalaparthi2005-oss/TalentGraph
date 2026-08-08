import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Loader2, AlertCircle } from 'lucide-react';

const OAuthCallbackPage = () => {
  const [searchParams] = useSearchParams();
  const { handleOAuthToken } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState(null);

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setError('OAuth authentication failed: No access token received.');
      return;
    }

    handleOAuthToken(token)
      .then(() => {
        navigate('/app', { replace: true });
      })
      .catch((err) => {
        setError(err.message || 'OAuth authentication failed. Please try logging in again.');
      });
  }, [searchParams, handleOAuthToken, navigate]);

  if (error) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4 text-center">
        <div className="w-12 h-12 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 flex items-center justify-center mb-4">
          <AlertCircle className="w-6 h-6" />
        </div>
        <h2 className="text-xl font-bold text-white mb-2">OAuth Authorization Error</h2>
        <p className="text-xs text-rose-300 max-w-sm mb-6">{error}</p>
        <button
          onClick={() => navigate('/login', { replace: true })}
          className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-xl transition-colors cursor-pointer"
        >
          Return to Login
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center text-slate-400">
      <Loader2 className="w-8 h-8 animate-spin text-cyan-400 mb-3" />
      <p className="text-sm font-medium tracking-wide text-slate-200">Completing Google Authentication...</p>
    </div>
  );
};

export default OAuthCallbackPage;
