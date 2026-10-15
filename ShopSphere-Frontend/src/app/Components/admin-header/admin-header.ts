import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../Services/auth-service';

@Component({
  selector: 'app-admin-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-header.html',
  styleUrls: ['./admin-header.css']
})
export class AdminHeader {
  private authService = inject(AuthService);

  // The header used to show a fixed name ("Muthulakshmi B" / "M") whoever was logged in.
  user = this.authService.currentUser;
  initials = this.authService.userInitials;

  logout(): void {
    this.authService.logout();
  }
}