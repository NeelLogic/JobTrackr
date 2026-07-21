import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  template: `
    <main class="page">
      <div class="page-heading">
        <div>
          <p class="eyebrow">Overview</p>
          <h1>Dashboard</h1>
          <p>Your application activity will appear here.</p>
        </div>
        <a class="button button--primary" routerLink="/applications/new">Add application</a>
      </div>
      <section class="empty-state card">
        <div class="empty-icon" aria-hidden="true">&#8599;</div>
        <h2>Ready for your first application</h2>
        <p>Add an opportunity to begin tracking your progress.</p>
        <a class="button button--primary" routerLink="/applications/new">Add application</a>
      </section>
    </main>
  `
})
export class Dashboard {}
