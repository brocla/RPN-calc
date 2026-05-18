# Purpose
The entering and displaying portions of the app are unsatisfactoy. 
I keep finding issues that were overlooked in the requirements document, so I am coming back to
re-do this portion, from scratch. 
It is my hope that the code is modular enough that this portion can be repaired without 
entangling the other bits.
Right now, I am considering a rewrite of logic/display/DisplayFormatter.kt and EntryStateMachine

Question. How do I know that I have specified every condition that will need to be considered. 
My approach is to think of the problem as a state machine.
The app already uses three states: Idle, Mantissa, and Exponent. 
These are catured in the EntryState class .
(Note: Mantissa is a misnomer. Mantissa means the first portion of number in scientific notation,
but its use here is really the Standard Notation or Standard Form. 
It uses the digits 0–9, with a decimal point if needed, without using exponents  )
I'm going to change the name of EntryState.mantissa to EntryState.standard.

I often used the tems state and mode interchangeably

# Description of the Display
The Display has 12 positions (like the HP41).
They are numbered 0 through 11.
0 is the leftmost position. 11 is the rightmost.
position 0 is reserved for the sigh character [-]
In standard mode, postions 1 thru 10 are used for digits and the decimal place. 
Position 11 is unused in Standard mode.

In Exponent mode The significand uses position 1 thru 8.
Position 9 is for the exponent sign.
Positions 10 and 11 are for the exponent.

The display can render the decimal point and commas without using a position. 
In the language of the font, these characters do not advance the width.
So the calculation of the length of a number does not include the point or the comma.

In both standard and exponent mode, numbers are loaded into the positions from left to right.
More on this later.


## Note on representing the display in code
In the notes that follow, the positions in the display are either called out by their position number or are referred to by a position counter or by a Pointer.
Position-counter language is used because it easier to write the spec and communicate the behavior. It may not be the best way to implment the display in code.
When the code is written, it is likely that it will actually be implemented with string operations.  For example, The standard mode might  be stored as:
- hasDecimal: Boolean — was the decimal key pressed?
- digits: String — integer-part keystrokes
- fracDigits: String — fractional-part keystrokes

No position counter will be needed. The lengths tell you whre you are.

So, I am keeping the notes in position-counter language (it is clearer for specification), but will implement in string-length language (it is simpler for code), 


## key groupings
So I can more easily refer to them later, I am defining names for groups of keys.
Digits [0 1 2 3 4 5 6 7 8 9]
Operators [Add	Subtract	Multiply	Divide	Reciprocal	Sqrt	Square	Pow10	Log	Exp	Ln	Power	Pi	Percent	PercentChange	Sin	Cos	Tan	ArcSin	ArcCos	ArcTan	NCr	NPr	Factorial	ToPolar	ToRect]
Formats [FixArg SciArg EngArg AllMode ]
StackManipulation [ENTER RollDown Clx Swap Lastx]
RegisterMemory [ Sto Rcl]



## Display on Startup
When the calculator starts it is in Idle mode.
The contents of the display are what ever is in the stack,
formattered by methods described later.




# Transitions

## idle → Entry
The keys that can transition the app out of Idle mode are:
[0 1 2 3 4 5 6 7 8 9 Decimal EEX]
(I am going to use this bracket notation to avoid using commas in the list)

### Behavior
The screen clears. Transition to Standard entry. The Decimal flag is cleared.
One of these...
- A Digit key puts that digit into position 1. The position counter is set to 2.
- The Decimal key puts a decimal point just before position 1. The position counter is set to 1. A decimal flag is set.
- EEX put the 1 digit in position 1 and enters transitions from Standard entry to Exp entry. The position counter is set to 10.


## Entry → Entry
The keys that stay in  Entry mode are:
[0 1 2 3 4 5 6 7 8 9 Decimal EEX CHS Backspace DegRad]

### Behavior
#### In Standard mode:
- A Digit key puts that digit at the position of the counter and increments the counter if the position counter <=10, otherwise, nothing happens>.
- The Decimal key puts a decimal point just before the position pointed to by the counter, if the decimal flag is clear, then it sets the flag. The position counter does not change. If the decimal flag was already set, nothing happens.
- EEX. if the value of the x register is zero, then clear the register, put 1 in position 1 and trasition to Exp mode. Otherwise, If the length of the integer portion of the standard number is > 8, do nothing. Otherwise, The mode switches to Exp. The ExpPointer is set to 10. Positions 9, 10, 11 are cleared.
(note: it is not a problem if the standard mode digits covering 9 or 10 or both are fractional digits, ie after the decimal point. They will cleared off the screen but remembered internally.)
- CHS. If the value in the x-register is zero, do nothing. otherwise, Toggle the sign in position 0. It should never be possible to display a negative sign on a zero. The position counter does not change.
- Backspace. If the decimal is between position n-1 and n, where n is the value of the postions counter, the decimal is removed. The decimal flag is cleared. Otherwise, decrement the position counter and remove that digit. If the position counter is now 1, then set the x register to zero and transition to Idle mode. Zero is displayed in the current format. 
`stackLiftEnabled` gets set to false  so the next digit press replaces X rather than lifting it.
This is the special case where the backspace key can transition to Idle.
- DegRad. Do nothing to the numeric display. Toggle the annunciator and set the appropriate angle mode.

#### In Exp mode:
- Digit key. If the ExpPointer = 10, put a digit there. Increment the pointer. If the ExpPointer == 11, put the digit there, increment the ExpPointer. If the ExpPointer > 11, then do nothing.
- Decimal. No-op
- EEX. No-op
- CHS. Toggle the sign in position 9.
- Backspace. If there is a digit in position 11, remove it. Set the the ExpPointer to 11.
Else, If there is a digit in position 10, remove it. Set the ExpPointer to 10.
Else, Unset the sign. Transition back to Standard mode. 
- DegRad. Do nothing to the numeric display. Toggle the annunciator and set the appropriate angle mode.


## Entry →  Idle
The keys that exit Entry mode and transition to Idle are...
[Operators StackManipulation RegisterMemory pi OpenConstants Formats]
under special condition, the backspace key can trasition from entry to Idle.

### Behavior
Operators. The value being entered becomes the value in the X-register. The operator selected performs it function leaving values on the stack. The values in the X and Y registers are formatted per the current format selected. Be in Idle mode.
StackManipulation. The value being entered becomes the value in the X-register. The operator selected performs it function leaving values on the stack. The values in the X and Y registers are formatted per the current format selected. Be in Idle mode.
STO. The value being entered becomes the value in the X-register. It is stored to the register chosen. The value is formatted. Now in Idle mode.
RCL. The value being enterd becomes the value in the X-register. The value retreive from memory pushes onto the stack. The old x value now displays in the Y register, per the current format.
The retrieved value in the X-register displays per the current format. Now in Idle.
pi. pi pushes onto the stack. The values on the stack display per the current format. Now in Idle.
OpenConstants. Same as pi.
Formats. The value being entered becomes the value in the X-register. The values in the X and Y registers are formatted per the new format. Now in Idle.

## Idle → Idle
The keys that operate on the display in Idle mode and return it to Idle mode are:
[Operators StackManipulation RegisterMemory pi OpenConstants Formats DegRad backspace] 

### Behavior
Operators, StackManipulation, RegisterMemory, pi, OpenConstants, Formats, DegRad. All of these perform their functino on the values on the stack, as they do. Then the values resulting on the stack are displayed in the current format (which might have changed).
backspace. Clears the x-register, like a Clx. formats the zero in the current format.

## Notes
Note: The shift key is not in any transition table. It's purely a latch that modifies the next key press.
Double Note: I have plans to convert the shift key from a latch to a toggle, but not yet.



** Comma insertion at display time **
 The zero-advance-width commas are added by insertThousandsCommas in the app layer, after the formatter returns a plain string. Commas are a rendering concern only — they must not be counted or stored as positions.



 # Formatting

 ## FIX
 The FIX command works like this,
- takes a single digit, N,  argument from the keyboard to indicate the number of digits after the decimal. N does not come from the stack
- turns off the other format annunciators and turns on the FIX annunciator
- then it reformats the display this way..
 - If the sign is negative, it is set in position 0.
 - If the integer part of the number is 10 or fewer digits, those digits are added to the display, starting from position 1, then the decimal, then the fractional part of the number, up to N digits  if there is space remaining in the 10 positions allocated. If the N fractional digits don't fit, the digits are rounded off until they do.
 - note: if the integer part of the number is empty or zero it is rendered as a single digit 0. There are never multiple leading zeros before the decimal.
 - if the number to render is too small or too large to fit in the display, switch to SCI format.  The N from FIX will carry over to SCI format.
 Numbers can be too small if there are toomany leading zero in the fractional part to leave at least one signigicant digit within the N digits allowed.
 Example N=2, value=0.001 → would show 0.00 with no significant digit visible → switches to SCI. 
 Numbers can be too big if the integer portion is more than 10 digits.
 Note: decimal and commas and sign do not add to the length.

## SCI
The SCI command works like this...
- takes a single digit, N,  argument from the keyboard to indicate the number of digits after the decimal. N does not come from the stack.
- turns off the other format annunciators and turns on the SCI annunciator
- then it reformats the display this way..
 - If the sign is negative, it is set in position 0.
 - THere are 8 positions available for the signficand. 1 - 8. This is less than FIX because some space is used for the Exponent.
 - The most significant digit goes in position 1, followed by a decimal point.
 - If N is 8 or 9, reduce it to 7. That is the max.
 -  The remaining digits fill out the fractional portion of the number up to N digits . Round the last digit if needed. If there are not enough digits to fill N digits the values are padded right with zeros.
 -  If there are not enough significant digits to fill all N positions, pad with zeros.  Example   SCI 4  2.34   will display as 2.3400

 -  the two digit exponent is written to position 10 and 11. If the exponent is a single digit, it is zero padded to the left. If the expoenent is negative the sign is set in position 9.
 -  if the exponent is greater than 99, that is an overflow error.
 -  if the exponent is less than -99, that is an underflow error.
 - note. rounding can happen to the left of the decimal if N=0. Example, the speed of light is 299,792,458 .   For N=0 this rounds to 3x10^8 and it displays as:

    |-------------|
    | 3.       08 |
    |_____________|
     0123456789ABC
  

## ENG
The ENG command works similar to SCI except the exponent must be a multiple of three. TO acheive this the number of digits to the left of the decimal are adjusted. There can be either 1, 2 or 3 of them. The go in positions 1, 1&2, or 1&2&3.  The number of fractional digits to the right of the decimal is still N but the cap may be different because the leading digits may take up more space. The total integer + fractional digits cannot exceed 8.
 -  if the exponent is greater than 99, that is an overflow error.
 -  if the exponent is less than -99, that is an underflow error.

## ALL
The ALL command works like this..
- It takes no arguments.
- displays up to 10 significant digits; trailing zeros after the last significant digit are suppressed.
- there are no trailing zeros
- the decimal point is only used when there are fractional digits
- uses scientific notation automatically when  the integer part would require more than 10 digit positions, or when leading zeros would consume more than N−1 fractional positions leaving no significant digit visible.
- the sign is shown in position 0 for negative numbers

Examples:
- 5 → 5
- 5.0 → 5
- 5.250000000000 → 5.25
- 1234567890.5 → 1.2345679 09   (switched to SCI 7)



## Zeros
Examples of zero in different formats
FIX 3    0.000
SCI 2    0.00      00
ENG 4    0.0000    00
ALL      0
pos     0123456789ABC