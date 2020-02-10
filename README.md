---
applications:
- name: pet-rescue-frontend
  path: ./frontend/build
  buildpack:
- name: pet-rescue-backend
  path: ./backend/build/libs/backend-0.0.1-SNAPSHOT.jar

