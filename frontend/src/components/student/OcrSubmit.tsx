/**
 * @file OcrSubmit.tsx
 * @brief Student submission interface for optical character recognition of handwritten homework.
 */

import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { UploadCloud, ShieldCheck, X, Sparkles, AlertCircle } from 'lucide-react';
import { submissionApi } from '../../api/submissionApi';
import { useUsage } from '../../hooks/useUsage';
import { useUIStore } from '../../store/uiStore';
import { LoadingSpinner } from '../common/LoadingSpinner';

/**
 * @brief Component allowing students to upload handwritten homework photos for zero-retention OCR evaluation.
 * @return JSX student OCR submission view.
 */
export const OcrSubmit: React.FC = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { remainingOcr } = useUsage();
  const { openUpgradeWall } = useUIStore();

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * @brief Event handler or helper executing handle file change.
   */
  const handleFileChange = (file: File) => {
    if (!file.type.startsWith('image/')) {
      setError('Please select a valid image file (JPEG or PNG).');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      setError('Image size exceeds 10MB limit.');
      return;
    }

    setError(null);
    setSelectedFile(file);
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
  };

  /**
   * @brief Event handler or helper executing handle drop.
   */
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileChange(e.dataTransfer.files[0]);
    }
  };

  /**
   * @brief Event handler or helper executing handle clear.
   */
  const handleClear = () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setSelectedFile(null);
    setPreviewUrl(null);
    setError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  /**
   * @brief Event handler or helper executing handle submit.
   */
  const handleSubmit = async () => {
    if (!selectedFile) return;

    if (remainingOcr <= 0) {
      openUpgradeWall('You have reached your daily OCR scan limit.');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const result = await submissionApi.submitImage(selectedFile);
      handleClear();
      navigate(`/student/review/${result.id}`);
    } catch (err: any) {
      console.error('OCR processing failed:', err);
      if (err.response?.status === 402 || err.response?.status === 429) {
        openUpgradeWall(err.response?.data?.message);
      } else {
        setError(err.response?.data?.message || 'Failed to process image. Ensure text is clear and well-lit.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">
          Handwritten Homework OCR
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Take a photo of your handwritten English notebook or worksheet for instant AI correction.
        </p>
      </div>

      <div className="p-4 rounded-2xl bg-indigo-50/70 border border-indigo-100 flex items-start space-x-3 text-xs text-indigo-950">
        <ShieldCheck className="w-5 h-5 text-primary flex-shrink-0 mt-0.5" />
        <div>
          <span className="font-bold">Zero-Retention Privacy Guarantee: </span>
          Your image is processed strictly in RAM and permanently deleted immediately after text extraction. We never save raw biometric photos or handwriting samples to disk or database.
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-center space-x-2">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        {!previewUrl ? (
          <div
            onDragOver={(e) => {
              e.preventDefault();
              setIsDragging(true);
            }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={`border-2 border-dashed rounded-3xl p-10 text-center cursor-pointer transition ${
              isDragging
                ? 'border-primary bg-indigo-50/40'
                : 'border-slate-200 hover:border-primary/50 hover:bg-slate-50'
            }`}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={(e) => e.target.files?.[0] && handleFileChange(e.target.files[0])}
              accept="image/png,image/jpeg,image/webp"
              className="hidden"
            />
            <div className="w-16 h-16 rounded-2xl bg-indigo-50 text-primary mx-auto flex items-center justify-center mb-4">
              <UploadCloud className="w-8 h-8" />
            </div>
            <h3 className="text-base font-bold text-slate-800 mb-1">
              Drag & Drop your homework photo here
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Supports JPEG, PNG up to 10MB
            </p>
            <button
              type="button"
              className="py-2.5 px-5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs transition"
            >
              Browse Photo
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="relative rounded-2xl overflow-hidden border border-slate-200 bg-slate-900/5 max-h-96 flex items-center justify-center">
              <img
                src={previewUrl}
                alt="Homework Preview"
                className="max-h-96 object-contain"
              />
              <button
                type="button"
                onClick={handleClear}
                className="absolute top-3 right-3 p-2 rounded-full bg-slate-900/70 hover:bg-slate-900 text-white transition backdrop-blur-sm"
                aria-label="Remove image"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <p className="text-xs text-slate-400 text-center">
              Image ready for zero-retention analysis ({selectedFile?.name})
            </p>
          </div>
        )}

        {selectedFile && (
          <div className="flex items-center space-x-3 pt-2">
            <button
              type="button"
              onClick={handleSubmit}
              disabled={isLoading}
              className="flex-1 flex items-center justify-center space-x-2 py-3.5 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
            >
              {isLoading ? (
                <LoadingSpinner size="sm" className="p-0 text-white" />
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Analyse Photo</span>
                </>
              )}
            </button>
            <button
              type="button"
              onClick={handleClear}
              disabled={isLoading}
              className="py-3.5 px-5 rounded-2xl border border-slate-200 hover:bg-slate-50 text-slate-600 font-semibold text-sm transition"
            >
              Clear Photo
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
