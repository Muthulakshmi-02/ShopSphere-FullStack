import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-terms-of-service',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './terms-of-service.html',
  styleUrls: ['../../shared/legal-page/legal-page.css']
})
export class TermsOfService {}
