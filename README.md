<html>
<head>
<title>ePubEditor</title>
<meta http-equiv="Content-Type" content="text/html;"/>
</head>
<body>
<p>I have two target audiences for ePubEditor: people who have created an e-book in HTML format, 
and now want a quick and easy way to turn it into an ePub, and people who have ePubs that do 
not display well on their devices because of "over-styling" and who want to clean up those 
ePubs for their own purposes. When you start the program some of the menu items are disabled, 
because no publication has been selected. If you select File->New, ePubEditor will start a new 
publication in the directory/folder you specify. At this point, the current tab will be the
Manifest table (new as of 10/23/11).</p> 
<p>The "Manifest" table will show a list of all the files 
that will be included in the publication. When you start a new ePub, you should see three files, 
ebook.css, toc.html, and cover.jpg. The "ebook.css" file is marked with a green checkmark
indicating that the default CSS file has be added to the publication structure. The remaining
two files should be marked with a red "stop"
symbol indicating that while they are listed in the manifest, they do not yet exist on the file 
system. You can remove them from the manifest, or better yet you can create them. Once the 
files exist (and the window is refreshed) the red "stop" will turn to a green "check."</p>
<p>At the bottom of the "Manifest" tab there is an "Add" button. When you press this button,
a file system browser will appear and allow you to add any file to the package. On this same 
tab there is also an "Edit" button. When a manifested file is highlighted and
this button is pressed, ePubEditor attempts to launch the editor associated with the declared 
media-type.</p>
<p>The association between Media-types and editors are stored in the properties file
associated with the program, ePubEditor.ini. On my computer, text and text/css point to TextPad 5, 
image/jpeg points to IrfanView, and application/xhtml+xml, text/html, and text/x-oeb1-document
all point to Microsoft's Visual Web Developer Express (that's the free one). By default, 
the only editor included in the properties file is
"C:\Program Files(x86)\Windows NT\Accessories\wordpad.exe". The Manifest shows all files that 
will become part of the publication, but only files listed in the "Content" will be presented 
to the reader. Order is unimportant in the Manifest, but items in the Content will be presented 
in the order they are listed. Therefore, on the Content tab there are buttons to move content 
up and down.</p>
<p>Metadata about the book can be entered or edited on the "Authors and Contributors"
 tab, or on the "Properties" tab. Every ePub must have at least one identifier which is
 globally unique, a title, and a language attribute. Once your publication is internally 
 consistent you may save it as an ePub.</p>
<p>Caution: if you have opened an ePub, and press "Save" your previous file will be replaced. 
If you're exploring the capabilities of the program, be sure to use "Save As..." and put your 
new file in some other location.</p>
</body>
</html> 
