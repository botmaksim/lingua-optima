export interface ProgressRecord {
  id: string;
  grammarTopic: string;
  totalAttempts: number;
  errorCount: number;
  masteryScore: number;
  lastPracticedAt: string;
}

export interface GroupProgress {
  groupId: string;
  records: ProgressRecord[];
}
