(function () {
  function payload(form) {
    var data = new FormData(form);
    var body = {};
    data.forEach(function (value, key) {
      if (typeof value === 'string' && value.trim() !== '') {
        body[key] = value.trim();
      }
    });
    return body;
  }

  document.addEventListener('submit', function (event) {
    var form = event.target;
    if (!form || !form.matches || !form.matches('[data-ddys-request-form]')) {
      return;
    }
    event.preventDefault();
    var status = form.querySelector('.ddys-open-request-form__status');
    if (status) {
      status.textContent = 'Submitting...';
    }
    fetch('/apis/api.ddys.io/v1alpha1/ddys/requests', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload(form))
    })
      .then(function (response) {
        return response.json().then(function (json) {
          if (!response.ok) {
            throw new Error(json.message || 'Submit failed.');
          }
          return json;
        });
      })
      .then(function () {
        form.reset();
        if (status) {
          status.textContent = 'Submitted.';
        }
      })
      .catch(function (error) {
        if (status) {
          status.textContent = error.message || 'Submit failed.';
        }
      });
  });
})();

