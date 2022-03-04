-- These have moved from the licence into licence_event to track the approval conversation.
alter table licence drop column reason_for_variation;
alter table licence drop column reason_for_referral;