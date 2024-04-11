'use strict';

$('#withdrawForm').submit(function (e) {
    e.preventDefault();
    let withdrawAmount = $('#withdrawAmount').val();
    $.ajax({
        url: '/customer-dashboard/banking-system/withdraw',
        method: 'POST',
        data: { amount: withdrawAmount },
        success: function (response) {
            // Assuming the response directly gives the new balance value
            $('.balance').text("$ "+response);


            // Show success message
            $('#message').addClass('alert-success text-center').text('Withdrawal operation is successful').removeClass('d-none').fadeIn().delay(4000).fadeOut(function() {
                $(this).addClass('d-none').removeClass('alert-success');
            });

        },
        error: function (jqXHR) {
            $('#withdrawError').text(jqXHR.responseText).fadeIn().delay(3000).fadeOut();
        }
    });
});

$('#depositForm').submit(function (e) {
    e.preventDefault();
    let depositAmount = $('#depositAmount').val();
    $.ajax({
        url: '/customer-dashboard/banking-system/deposit',
        method: 'POST',
        data: { amount: depositAmount },
        success: function (response) {
            // Assuming the response directly gives the new balance value
            $('.balance').text("$ "+response);

            // Show success message
            $('#message').addClass('alert-success text-center').text('Deposit operation is successful').removeClass('d-none').fadeIn().delay(4000).fadeOut(function() {
                $(this).addClass('d-none').removeClass('alert-success');
            });
        },
        error: function (jqXHR) {
            $('#depositError').text(jqXHR.responseText).fadeIn().delay(3000).fadeOut();
        }
    });
});

$('#transferForm').submit(function (e) {
    e.preventDefault();
    let transferAmount = $('#transferAmount').val();
    let transferTo = $('#transferTo').val();

    $.ajax({
        url: '/customer-dashboard/banking-system/transfer',
        method: 'POST',
        data: { amount: transferAmount ,transferTo:transferTo},
        success: function (response) {
            $('.balance').text("$ "+response);
            // Show success message
            $('#message').addClass('alert-success text-center').text(transferAmount+ ' Transferred successfully to '+transferTo ).removeClass('d-none').fadeIn().delay(4000).fadeOut(function() {
                $(this).addClass('d-none').removeClass('alert-success');
            });

        },
        error: function (jqXHR) {
            $('#transferError').text(jqXHR.responseText).fadeIn().delay(3000).fadeOut();

        }
    });
});

$('#closeForm').submit(function (e) {
    e.preventDefault();
    let user = $('#user').val();
    let pin = $('#pin').val();
    let hashedPin = sha256(pin);

    $.ajax({
        url: '/customer-dashboard/banking-system/close',
        method: 'POST',
        data: { user: user ,pin:pin},
        success: function (response) {
            $.ajax({
                url: '/logout',
                method: 'POST',
            });            },
        error: function (jqXHR) {
            $('#closeError').text(jqXHR.responseText).fadeIn().delay(3000).fadeOut();
        }
    });
});


const timer = document.querySelector('.timer_time');
const LogOutTimer = function () {
    let time = 240;
    let ti = setInterval(function () {
        let sec = String(time % 60).padStart(2, '0');
        let min = String(Math.trunc(time / 60)).padStart(2, '0');
        timer.innerHTML = `${min}:${sec}`;
        if (time === 0) {
            clearInterval(ti);
            $.ajax({
                url: '/logout',
                method: 'POST',
            });
        }
        time--;
    }, 1000);
    return ti;
};
let logoutInterval = LogOutTimer();

const resetLogoutTimer = function () {
    clearInterval(logoutInterval);
    logoutInterval = LogOutTimer();
};

$(document).ready(function () {
    resetLogoutTimer();
});

