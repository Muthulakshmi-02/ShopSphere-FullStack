export interface ProductQuestionResponse {
  id: number;
  productId: number;
  askerName: string;
  question: string;
  answer?: string | null;
  answeredBy?: string | null;
  createdAt: string;
  answeredAt?: string | null;
}

export interface QuestionRequest {
  question: string;
}

export interface AnswerRequest {
  answer: string;
}
