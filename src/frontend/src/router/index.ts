import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import PeopleView from '@/views/people/PeopleView.vue'
import StatisticsView from '@/views/statistics/StatisticsView.vue'
import SchoolsView from '@/views/schools/SchoolsView.vue'
import SubjectsView from '@/views/subjects/SubjectsView.vue'
import ExamUploadView from '@/views/exams/ExamUploadView.vue'
import ExamsView from '@/views/exams/ExamsView.vue'
import ExamSegmentView from '@/views/exams/ExamSegmentView.vue'
import GradingInboxView from '@/views/grading/GradingInboxView.vue'
import GradingDetailView from '@/views/grading/GradingDetailView.vue'
import MyExamsView from '@/views/student/MyExamsView.vue'
import MyExamDetailView from '@/views/student/MyExamDetailView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true }
    },
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/people',
      name: 'people',
      component: PeopleView,
      meta: { permission: 'PERSON_READ' }
    },
    {
      path: '/statistics',
      name: 'statistics',
      component: StatisticsView,
      meta: { permission: 'STATISTICS_READ' }
    },
    {
      path: '/schools',
      name: 'schools',
      component: SchoolsView,
      meta: { permission: 'SCHOOL_READ' }
    },
    {
      path: '/subjects',
      name: 'subjects',
      component: SubjectsView,
      meta: { permission: 'SUBJECT_READ' }
    },
    {
      path: '/exams',
      name: 'exams',
      component: ExamsView,
      meta: { permission: 'EXAM_READ' }
    },
    {
      path: '/exams/upload',
      name: 'exam-upload',
      component: ExamUploadView,
      meta: { permission: 'EXAM_CREATE' }
    },
    {
      path: '/exams/:id/segment',
      name: 'exam-segment',
      component: ExamSegmentView,
      meta: { permission: 'EXAM_READ' }
    },
    {
      path: '/grading',
      name: 'grading',
      component: GradingInboxView,
      meta: { permission: 'EVALUATE_READ' }
    },
    {
      path: '/grading/:id',
      name: 'grading-detail',
      component: GradingDetailView,
      meta: { permission: 'EVALUATE_READ' }
    },
    {
      path: '/my-exams',
      name: 'my-exams',
      component: MyExamsView,
      meta: { permission: 'EXAM_CONSULT' }
    },
    {
      path: '/my-exams/:id',
      name: 'my-exam-detail',
      component: MyExamDetailView,
      meta: { permission: 'EXAM_CONSULT' }
    }
  ]
})

// Auth guard: /login is public; everything else needs a valid session, and a
// route may additionally require a permission via meta.permission.
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.isAuthenticated && to.name === 'login') {
      return { name: 'home' }
    }
    return true
  }

  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const required = to.meta.permission as string | undefined
  if (required && !auth.hasPermission(required)) {
    return { name: 'home' }
  }

  return true
})

export default router
