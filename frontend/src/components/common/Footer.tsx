import React from 'react';
import { Link } from 'react-router-dom';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-white border-t border-slate-200 mt-auto py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center space-x-2">
            <span className="font-bold text-slate-800">Lingua Optima</span>
            <span className="text-slate-400">·</span>
            <span className="text-xs text-slate-500">
              Zero-retention OCR & Adaptive AI English Mastery
            </span>
          </div>

          <div className="flex items-center space-x-6 text-sm text-slate-500">
            <a href="#privacy" className="hover:text-primary transition">
              Privacy Policy
            </a>
            <a href="#terms" className="hover:text-primary transition">
              Terms of Service
            </a>
            <a href="#help" className="hover:text-primary transition">
              Help Center
            </a>
            <Link to="/subscription" className="hover:text-primary transition">
              Pricing
            </Link>
          </div>

          <div className="text-xs text-slate-400">
            © {new Date().getFullYear()} Lingua Optima. All rights reserved.
          </div>
        </div>
      </div>
    </footer>
  );
};
