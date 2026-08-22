export interface NavItem {
  name: string
  path: string
  icon: string
  permission: string
}

export const navItems: NavItem[] = [
  { name: 'Pessoal', path: '/people', icon: 'mdi-account-group', permission: 'PERSON_READ' },
  { name: 'Escolas', path: '/schools', icon: 'mdi-school', permission: 'SCHOOL_READ' },
  { name: 'Disciplinas', path: '/subjects', icon: 'mdi-book-open-variant', permission: 'SUBJECT_READ' },
  { name: 'Exames', path: '/exams', icon: 'mdi-file-document-multiple', permission: 'EXAM_READ' },
  { name: 'As minhas provas', path: '/my-exams', icon: 'mdi-file-eye', permission: 'EXAM_CONSULT' },
  { name: 'Correcções', path: '/grading', icon: 'mdi-clipboard-check', permission: 'EVALUATE_READ' },
  { name: 'Estatísticas', path: '/statistics', icon: 'mdi-chart-bar', permission: 'STATISTICS_READ' },
]
