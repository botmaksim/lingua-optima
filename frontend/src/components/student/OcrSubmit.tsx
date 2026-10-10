/**
 * @file OcrSubmit.tsx
 * @brief Student submission interface for optical character recognition of multi-page handwritten homework.
 */

import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { UploadCloud, ShieldCheck, X, Sparkles, AlertCircle, Plus, FileImage } from 'lucide-react';
import { submissionApi } from '../../api/submissionApi';
import { useUsage } from '../../hooks/useUsage';
import { useUIStore } from '../../store/uiStore';
import { LoadingSpinner } from '../common/LoadingSpinner';

const MAX_PHOTOS = 5;
const MAX_SINGLE_BYTES = 10 * 1024 * 1024; // 10MB
const MAX_TOTAL_BYTES = 25 * 1024 * 1024;  // 25MB

interface PreviewItem {
  file: File;
  previewUrl: string;
}

/**
 * @brief Component allowing students to upload multi-page handwritten homework photos for zero-retention OCR evaluation.
 * @return JSX student OCR submission view.
 */
export const OcrSubmit: React.FC = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { remainingOcr } = useUsage();
  const { openUpgradeWall } = useUIStore();

  const [previewItems, setPreviewItems] = useState<PreviewItem[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Clean up object URLs on unmount
  useEffect(() => {
    return () => {
      previewItems.forEach((item) => URL.revokeObjectURL(item.previewUrl));
    };
  }, []);

  const totalBytes = previewItems.reduce((acc, item) => acc + item.file.size, 0);

  /**
   * @brief Validates and attaches newly selected files to preview items.
   */
  const handleFilesAdded = (incomingFiles: FileList | File[]) => {
    const filesArray = Array.from(incomingFiles);
    if (filesArray.length === 0) return;

    if (previewItems.length + filesArray.length > MAX_PHOTOS) {
      setError(`You can upload at most ${MAX_PHOTOS} photos for one homework submission.`);
      return;
    }

    let runningTotal = totalBytes;
    const newItems: PreviewItem[] = [];

    for (const file of filesArray) {
      if (!file.type.startsWith('image/')) {
        setError(`File "${file.name}" is not a supported image format (JPEG, PNG, WebP).`);
        return;
      }
      if (file.size > MAX_SINGLE_BYTES) {
        setError(`File "${file.name}" exceeds the 10MB single photo limit.`);
        return;
      }
      runningTotal += file.size;
      if (runningTotal > MAX_TOTAL_BYTES) {
        setError(`Total homework upload size exceeds the 25MB limit.`);
        return;
      }
      newItems.push({
        file,
        previewUrl: URL.createObjectURL(file),
      });
    }

    setError(null);
    setPreviewItems((prev) => [...prev, ...newItems]);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  /**
   * @brief Removes a single photo by index.
   */
  const handleRemovePhoto = (index: number) => {
    setPreviewItems((prev) => {
      const target = prev[index];
      if (target) {
        URL.revokeObjectURL(target.previewUrl);
      }
      return prev.filter((_, i) => i !== index);
    });
    setError(null);
  };

  /**
   * @brief Drag-and-drop handler.
   */
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files) {
      handleFilesAdded(e.dataTransfer.files);
    }
  };

  /**
   * @brief Clears all uploaded photos.
   */
  const handleClearAll = () => {
    previewItems.forEach((item) => URL.revokeObjectURL(item.previewUrl));
    setPreviewItems([]);
    setError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  /**
   * @brief Dispatches the homework photos for multi-page OCR and grading.
   */
  const handleSubmit = async () => {
    if (previewItems.length === 0) return;

    if (remainingOcr < previewItems.length) {
      openUpgradeWall(`You have ${remainingOcr} OCR scans left, but this submission requires ${previewItems.length} scans.`);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const filesToSubmit = previewItems.map((item) => item.file);
      const result = await submissionApi.submitImage(filesToSubmit);
      handleClearAll();
      navigate(`/student/review/${result.id}`);
    } catch (err: any) {
      console.error('OCR processing failed:', err);
      if (err.response?.status === 402 || err.response?.status === 429) {
        openUpgradeWall(err.response?.data?.message);
      } else {
        setError(err.response?.data?.message || 'Failed to process images. Ensure text is clear and well-lit.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">
          Handwritten Homework OCR
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Take photos of your handwritten English notebook or worksheet (up to 5 pages) for instant AI correction.
        </p>
      </div>

      <div className="p-4 rounded-2xl bg-indigo-50/70 border border-indigo-100 flex items-start space-x-3 text-xs text-indigo-950">
        <ShieldCheck className="w-5 h-5 text-primary flex-shrink-0 mt-0.5" />
        <div>
          <span className="font-bold">Zero-Retention Privacy Guarantee: </span>
          Your photos are processed strictly in RAM and permanently purged immediately after text extraction. We never save raw biometric photos or handwriting samples to disk or database.
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-center space-x-2">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        <input
          type="file"
          ref={fileInputRef}
          onChange={(e) => e.target.files && handleFilesAdded(e.target.files)}
          accept="image/png,image/jpeg,image/webp"
          multiple
          className="hidden"
        />

        {previewItems.length === 0 ? (
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
            <div className="w-16 h-16 rounded-2xl bg-indigo-50 text-primary mx-auto flex items-center justify-center mb-4">
              <UploadCloud className="w-8 h-8" />
            </div>
            <h3 className="text-base font-bold text-slate-800 mb-1">
              Drag & Drop your homework photo(s) here
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Upload up to 5 pages (JPEG, PNG, WebP &middot; Max 10MB per photo, 25MB total)
            </p>
            <button
              type="button"
              className="py-2.5 px-5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs transition"
            >
              Browse Photos
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <div className="flex items-center space-x-2">
                <FileImage className="w-4 h-4 text-primary" />
                <span className="text-sm font-bold text-slate-800">
                  Pages Selected: {previewItems.length} / {MAX_PHOTOS}
                </span>
                <span className="text-xs text-slate-400">
                  ({(totalBytes / (1024 * 1024)).toFixed(1)} MB / 25 MB)
                </span>
              </div>
              {previewItems.length < MAX_PHOTOS && (
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="inline-flex items-center space-x-1 text-xs font-semibold text-primary hover:text-primary-hover transition"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add Page</span>
                </button>
              )}
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {previewItems.map((item, index) => (
                <div
                  key={index}
                  className="relative rounded-2xl overflow-hidden border border-slate-200 bg-slate-900/5 group aspect-[3/4] flex flex-col justify-between"
                >
                  <img
                    src={item.previewUrl}
                    alt={`Page ${index + 1}`}
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute top-2 left-2 px-2 py-0.5 rounded-lg bg-slate-900/80 backdrop-blur-sm text-white text-[11px] font-bold">
                    Page {index + 1}
                  </div>
                  <button
                    type="button"
                    onClick={() => handleRemovePhoto(index)}
                    className="absolute top-2 right-2 p-1.5 rounded-full bg-slate-900/70 hover:bg-rose-600 text-white transition backdrop-blur-sm"
                    aria-label={`Remove page ${index + 1}`}
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                  <div className="absolute bottom-0 inset-x-0 bg-gradient-to-t from-slate-950/80 via-slate-950/40 to-transparent p-2 text-[11px] text-white truncate">
                    {item.file.name}
                  </div>
                </div>
              ))}

              {previewItems.length < MAX_PHOTOS && (
                <div
                  onClick={() => fileInputRef.current?.click()}
                  className="rounded-2xl border-2 border-dashed border-slate-200 hover:border-primary/50 hover:bg-slate-50 cursor-pointer flex flex-col items-center justify-center p-4 aspect-[3/4] text-slate-400 hover:text-primary transition"
                >
                  <Plus className="w-8 h-8 mb-2" />
                  <span className="text-xs font-bold">Add Next Page</span>
                  <span className="text-[10px] text-slate-400 mt-1">Page {previewItems.length + 1}</span>
                </div>
              )}
            </div>
          </div>
        )}

        {previewItems.length > 0 && (
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
                  <span>Analyse {previewItems.length} {previewItems.length === 1 ? 'Page' : 'Pages'}</span>
                </>
              )}
            </button>
            <button
              type="button"
              onClick={handleClearAll}
              disabled={isLoading}
              className="py-3.5 px-5 rounded-2xl border border-slate-200 hover:bg-slate-50 text-slate-600 font-semibold text-sm transition"
            >
              Clear All
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
