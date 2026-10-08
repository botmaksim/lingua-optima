import { User } from './user';

export interface GroupStudent {
  id: string;
  student: User;
  isActive: boolean;
  joinedAt: string;
}

export interface Group {
  id: string;
  name: string;
  studentCount: number;
  avgScore?: number;
  students?: User[];
  createdAt: string;
}

export interface CreateGroupRequest {
  name: string;
}
