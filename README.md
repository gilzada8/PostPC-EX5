# PostPC-EX6

app:
Honey im home let you save your home location, and set phone number so that whenever you reach to
your home, SMS will be sent to the phone you set.

Question:
Currently, every time we send an SMS we also show notification "sending sms: .....".
What should we add in our code-base so that when the SMS will get delivered, this notification's text will be changed to "sms sent: ......"?

Answer:
We would need to add to the PendingIntent extras the Notification ID, and set its action name (for example, "smsDelivered").
Than, add action to LocalSendSmsBroadcastReceiver that whenever "smsDelivered" happened, get the notification ID
from its extras and update that notification text to "sms sent: ......"

I have written the code alone.

Gil

![alt text](/screenshot.png)