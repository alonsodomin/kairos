define ['knockout', 'jquery'], (ko, $) ->
  class ScheduledJobsView
    constructor: () ->
      @baseUri = '/scheduler'
      @schedules = ko.observableArray()
      @initialize()

    initialize: () ->
      $.get @baseUri + '/jobs', (response) =>
        $.each response, (idx, item) =>
          @schedules.push item