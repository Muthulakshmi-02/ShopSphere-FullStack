import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../Models/User.model';
import { AnswerRequest, ProductQuestionResponse, QuestionRequest } from '../Models/Question.model';
import { API_BASE_URL } from '../shared/api.config';

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/api`;

  getQuestions(productId: number): Observable<ApiResponse<ProductQuestionResponse[]>> {
    return this.http.get<ApiResponse<ProductQuestionResponse[]>>(`${this.baseUrl}/products/${productId}/questions`);
  }

  askQuestion(productId: number, request: QuestionRequest): Observable<ApiResponse<ProductQuestionResponse>> {
    return this.http.post<ApiResponse<ProductQuestionResponse>>(`${this.baseUrl}/products/${productId}/questions`, request);
  }

  answerQuestion(questionId: number, request: AnswerRequest): Observable<ApiResponse<ProductQuestionResponse>> {
    return this.http.put<ApiResponse<ProductQuestionResponse>>(`${this.baseUrl}/questions/${questionId}/answer`, request);
  }

  deleteQuestion(questionId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/questions/${questionId}`);
  }
}
