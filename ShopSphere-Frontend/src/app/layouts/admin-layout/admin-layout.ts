import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AdminHeader } from '../../Components/admin-header/admin-header';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    AdminHeader
  ],
  templateUrl: './admin-layout.html',
  styleUrls: ['./admin-layout.css']
})
export class AdminLayout { }
