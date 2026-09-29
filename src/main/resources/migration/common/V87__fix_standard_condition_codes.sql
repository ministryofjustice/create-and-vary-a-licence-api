-- Fix standard condition codes so they match the current standard condition codes in the system.

update standard_condition
set condition_code = 'e670ac69-eda2-4b04-a0a1-a3c8492fe1e6'
where condition_text =
      'Get permission from your supervising officer to stay at an address and if you want to stay somewhere else for one or more nights.'
  and condition_version = '4.0';

update standard_condition
set condition_code = '3361683a-504a-4357-ae22-6aa01b370b4a'
where condition_text =
      'Keep in touch and meet with your supervising officer in the way they tell you to. This includes meeting them where you live.'
  and condition_version = '4.0';

update standard_condition
set condition_code = '78A5F860-4791-48F2-B707-D6D4413850EE'
where condition_text =
      'Tell your supervising officer about any names you use that are different to the names on this licence.'
  and condition_version = '4.0';

update standard_condition
set condition_code = '6FA6E492-F0AB-4E76-B868-63813DB44696'
where condition_text =
      'Inform your supervising officer if your contact details change. For example, your phone number or email address.'
  and condition_version = '4.0';

update standard_condition
set condition_code = '88069445-08cb-4f16-915f-5a162d085c26'
where condition_text =
      'Tell your supervising officer about any new work, or a type of work, you want to do. Get their approval before you start this work.'
  and condition_version = '4.0';

update standard_condition
set condition_code = '7d416906-0e94-4fde-ae86-8339d339ccb7'
where condition_text =
      'Get permission from your supervising officer if you want to leave the United Kingdom, Isle of Man or the Channel Islands. This does not apply if you are being deported or removed for immigration purposes.'
  and condition_version = '4.0';

update standard_condition
set condition_code = '21E5216D-C601-4282-A221-697834D0C7C4'
where condition_text =
      'Get permission from your supervising officer if you want to apply for a new passport. If requested, tell your supervising officer about any passports you have already.'
  and condition_version = '4.0';
