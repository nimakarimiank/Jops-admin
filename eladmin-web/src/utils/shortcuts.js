import Date from './datetime.js'

export const calendarBaseShortcuts = [{
  text: 'Today',
  onClick(picker) {
    const startTime = new Date(new Date().setHours(0, 0, 0))
    const endTime = new Date(new Date().setHours(23, 59, 59))
    picker.$emit('pick', [startTime, endTime])
  }
}, {
  text: 'Yesterday',
  onClick(picker) {
    const startTime = new Date(new Date().daysAgo(1).setHours(0, 0, 0))
    const endTime = new Date(new Date().daysAgo(1).setHours(23, 59, 59))
    picker.$emit('pick', [startTime, endTime])
  }
}, {
  text: 'This Week',
  onClick(picker) {
    const startTime = new Date(new Date().daysAgo(new Date().getDay() - 1).setHours(0, 0, 0))
    const endTime = new Date(new Date().setHours(23, 59, 59))
    picker.$emit('pick', [startTime, endTime])
  }
}, {
  text: 'This Month',
  onClick(picker) {
    const startTime = new Date(new Date().monthBegin().setHours(0, 0, 0))
    const endTime = new Date(new Date().setHours(23, 59, 59))
    picker.$emit('pick', [startTime, endTime])
  }
}, {
  text: 'Current Quarter',
  onClick(picker) {
    const startTime = new Date(new Date().quarterBegin().setHours(0, 0, 0))
    const endTime = new Date(new Date().setHours(23, 59, 59))
    picker.$emit('pick', [startTime, endTime])
  }
}, {
  text: 'Last 30 Days',
  onClick(picker) {
    const startTime = new Date(new Date().daysAgo(30).setHours(0, 0, 0))
    const endTime = new Date(new Date().setHours(23, 59, 59))
    picker.$emit('pick', [startTime, endTime])
  }
}]

export const calendarMoveShortcuts = [{
  text: '‹ Previous Day ',
  onClick(picker) {
    let startTime = new Date(new Date().setHours(0, 0, 0))
    let endTime = new Date(new Date().setHours(23, 59, 59))
    if (!picker.value) {
      picker.value = [startTime, endTime]
    }
    startTime = picker.value[0].daysAgo(1)
    endTime = picker.value[1].daysAgo(1)
    picker.$emit('pick', [startTime, endTime])
  }
}, {
  text: ' Next Day ›',
  onClick(picker) {
    let startTime = new Date(new Date().setHours(0, 0, 0))
    let endTime = new Date(new Date().setHours(23, 59, 59))
    if (!picker.value) {
      picker.value = [startTime, endTime]
    }
    startTime = picker.value[0].daysAgo(-1)
    endTime = picker.value[1].daysAgo(-1)
    picker.$emit('pick', [startTime, endTime])
  }
}]

export const calendarShortcuts = [
  ...calendarBaseShortcuts,
  ...calendarMoveShortcuts
]
