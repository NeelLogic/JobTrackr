import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
@Component({selector:'app-application-list',imports:[RouterLink],template:`<main class="page"><div class="page-heading"><div><p class="eyebrow">Pipeline</p><h1>Applications</h1><p>Your applications will be listed here.</p></div><a class="button button--primary" routerLink="/applications/new">Add application</a></div><section class="empty-state card"><h2>No applications yet</h2><p>Add your first role to start building your pipeline.</p></section></main>`})
export class ApplicationList {}
