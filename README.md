# ePubEditor

**ePubEditor** is a Java program designed to create and edit ePub files in their native form.
Its UI is written using Java Swing, although the intent is to create a new fork which will
use JavaFX for its user interface.

**ePubEditor** currently supports ePub version 2.x, but work is underway to add support for
ePub versions 3.x.

I have two target audiences for ePubEditor: people who have created an e-book in HTML format, 
and now want a quick and easy way to turn it into an ePub, and people who have ePubs that do 
not display well on their devices because of "over-styling" and who want to clean up those 
ePubs for their own purposes.

User documentation for **ePubEditor** can be found at [https://lpassey.github.io/ePubEditor](https://lpassey.github.io/ePubEditor).

For instructions on how to create a new ePub, see [https://lpassey.github.io/ePubEditor#new](https://lpassey.github.io/ePubEditor#new).

For instructions on how to import, edit and clean an existing ePub, see [https://lpassey.github.io/ePubEditor#import](https://lpassey.github.io/ePubEditor#import)

As always, project and source code files can be foud [here](https://www.github.com/lpassey/ePubEditor).

**Update:** Changes to the W3C EPUBCheck library has rendered it virtually unusable by 
programs such as ePubEditor. Given this fact, references to EPUBCheck from inside ePubEditor
have been removed. It is possible to fork and re-implement the programatic interface, but
that is a low priority item. For those committed to using EPUBCheck you must compile
  your ePubs first, then 
  run the EPUBCheck utility from the command line.

