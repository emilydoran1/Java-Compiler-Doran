%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% CMPT 432
% Spring 2021
% Lab One
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Short Sectioned Assignment
% LaTeX Template
% Version 1.0 (5/5/12)
%
% This template has been downloaded from: http://www.LaTeXTemplates.com
% Original author: % Frits Wenneker (http://www.howtotex.com)
% License: CC BY-NC-SA 3.0 (http://creativecommons.org/licenses/by-nc-sa/3.0/)
% Modified by Alan G. Labouseur  - alan@labouseur.com
% Modified from Alan's version for lab1 by Emily Doran - emily.doran1@marist.edu
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%----------------------------------------------------------------------------------------
%	PACKAGES AND OTHER DOCUMENT CONFIGURATIONS
%----------------------------------------------------------------------------------------

\documentclass[letterpaper, 10pt,DIV=13]{scrartcl} 

\usepackage[T1]{fontenc} % Use 8-bit encoding that has 256 glyphs
\usepackage[english]{babel} % English language/hyphenation
\usepackage{amsmath,amsfonts,amsthm,xfrac} % Math packages
\usepackage{sectsty} % Allows customizing section commands
\usepackage{graphicx}
\usepackage[lined,linesnumbered,commentsnumbered]{algorithm2e}
\usepackage{listings}
\usepackage{parskip}
\usepackage{lastpage}

\allsectionsfont{\normalfont\scshape} % Make all section titles in default font and small caps.

\usepackage{fancyhdr} % Custom headers and footers
\pagestyle{fancyplain} % Makes all pages in the document conform to the custom headers and footers

\fancyhead{} % No page header - if you want one, create it in the same way as the footers below
\fancyfoot[L]{} % Empty left footer
\fancyfoot[C]{} % Empty center footer
\fancyfoot[R]{page \thepage\ of \pageref{LastPage}} % Page numbering for right footer

\renewcommand{\headrulewidth}{0pt} % Remove header underlines
\renewcommand{\footrulewidth}{0pt} % Remove footer underlines
\setlength{\headheight}{13.6pt} % Customize the height of the header

\numberwithin{equation}{section} % Number equations within sections (i.e. 1.1, 1.2, 2.1, 2.2 instead of 1, 2, 3, 4)
\numberwithin{figure}{section} % Number figures within sections (i.e. 1.1, 1.2, 2.1, 2.2 instead of 1, 2, 3, 4)
\numberwithin{table}{section} % Number tables within sections (i.e. 1.1, 1.2, 2.1, 2.2 instead of 1, 2, 3, 4)

\setlength\parindent{0pt} % Removes all indentation from paragraphs.

\binoppenalty=3000
\relpenalty=3000

%----------------------------------------------------------------------------------------
%	TITLE SECTION
%----------------------------------------------------------------------------------------

\newcommand{\horrule}[1]{\rule{\linewidth}{#1}} % Create horizontal rule command with 1 argument of height

\title{	
   \normalfont \normalsize 
   \textsc{CMPT 432 - Spring 2021 - Dr. Labouseur} \\[10pt] % Header stuff.
   \horrule{0.5pt} \\[0.25cm] 	% Top horizontal rule
   \huge Lab One  \\     	    % Assignment title
   \horrule{0.5pt} \\[0.25cm] 	% Bottom horizontal rule
}

\author{Emily Doran \\ \normalsize Emily.Doran1@Marist.edu}

\date{\normalsize\today} 	% Today's date.

\begin{document}
\maketitle % Print the title

\section*{Crafting A Compiler}
\subsection*{1.11 (MOSS)}

\normalsize\textbf{Investigate the techniques MOSS uses to ﬁnd similarity. How does MOSS diﬀer from other approaches for detecting possible plagiarism?}

\normalsize{The Measure of Software Similarity, better known as MOSS, is a system that is used to help detect plagiarism by detecting the similarities between code. Aside from being able to find similarities between code written in different languages, what makes MOSS unique is that it also compares the tokens of the program. MOSS returns the number of tokens matched between programs, as well as the number of lines matched.}

\subsection*{3.1 (Token Sequence)}
\normalsize\textbf{Assume the following text is presented to a C scanner:}

\lstset{numbers=left, numberstyle=\tiny, stepnumber=1, numbersep=5pt, basicstyle=\footnotesize\ttfamily}
\begin{lstlisting}[frame=single, ]  
main(){
    const float payment = 384.00;
    float bal;
    int month = 0;
    bal=15000;
    while (bal>0){
        printf("Month: %2d Balance: %10.2f\n", month, bal);
        bal=bal-payment+0.015*bal;
        month=month+1;
    }
}
\end{lstlisting}

\normalsize\textbf{What token sequence is produced? For which tokens must extra information be returned in addition to the token code?}

\begin{verbatim}
FUNCTION [ main ] found at (1:1)
L_PAREN [ ( ] found at (1:5)
R_PAREN [ ( ] found at (1:6)
L_BRACE [ { ] found at (1:7)
CONST [ const ] found at (2:5)
TYPE [ float ] found at (2:11)
ID [ payment ] found at (2:17)
ASSIGN_OP [ = ] found at (2:25)
DIGIT [ 384.00 ] found at (2:27)
PUNCT [ ; ] found at (2:33)
TYPE [ float ] found at (3:5)
ID [ bal ] found at (3:11)
PUNCT [ ; ] found at (3:14)
TYPE [ int ] found at (4:5)
ID [ month ] found at (4:9)
ASSIGN_OP [ = ] found at (4:15)
DIGIT [ 0 ] found at (4:17)
PUNCT [ ; ] found at (4:18)
ID [ bal ] found at (5:5)
ASSIGN_OP [ = ] found at (5:8)
DIGIT [ 15000 ] found at (5:9)
PUNCT [ ; ] found at (5:14)
WHILE [ while ] found at (6:5)
L_PAREN [ ( ] found at (6:11)
ID [ bal ] found at (6:12)
GREATER_THAN [ > ] found at (6:15)
DIGIT [ 0 ] found at (6:16)
R_PAREN [ ( ] found at (6:17)
L_BRACE [ { ] found at (6:18)
PRINTF [ printf ] found at (7:9)
L_PAREN [ ( ] found at (7:10)
STRING [ "Month: %2d Balance: %10.2f\n" ] found at (7:10)
PUNCT [ , ] found at (7:40)
ID [ month ] found at (7:42)
PUNCT [ , ] found at (7:47)
ID [ bal ] found at (7:49)
R_PAREN [ ) ] found at (7:52)
PUNCT [ ; ] found at (7:53)
ID [ bal ] found at (8:9)
ASSIGN_OP [ = ] found at (8:12)
ID [ bal ] found at (8:13)
SUBTRACT_OP [ - ] found at (8:16)
ID [ payment ] found at (8:17)
ADDITION_OP [ + ] found at (8:24)
DIGIT [ 0.015 ] found at (8:25)
MULTIPLY_OP [ * ] found at (8:30)
ID [ bal ] found at (8:31)
PUNCT [ ; ] found at (8:34)
ID [ month ] found at (9:9)
ASSIGN_OP [ = ] found at (9:14)
ID [ month ] found at (9:15)
ADDITION_OP [ + ] found at (9:20)
DIGIT [ 1 ] found at (9:21)
PUNCT [ ; ] found at (9:22)
R_BRACE [ } ] found at (10:5)
R_BRACE [ } ] found at (11:1)
\end{verbatim}
\normalsize{For identifier tokens, we need to store some additional information because the scanner doesn't know when an identifier should be entered into the symbol table or if it should return a pointer. }

\section*{Dragon}
\subsection*{1.1.4 (Advantages of C as a Target Language)}
\normalsize\textbf{A compiler that translates a high-level language into another high-level language is called a \textit{source-to-source} translator. What advantages are there to using C as a target language for a compiler?}

\normalsize{C is a good target language for a compiler because it is low level, so it isn't necessary for the compiler to translate to machine code. Also, since C is such a widely used language, the compilers can be used on many different platforms. }

\subsection*{1.6.1 (Variables in Block-Structured Code)}
\normalsize\textbf{For the block-structured C code of Fig. 1.13(a) below, indicate the values assigned to \textit{w, x, y,} and \textit{z}.}

\lstset{numbers=left, numberstyle=\tiny, stepnumber=1, numbersep=5pt, basicstyle=\footnotesize\ttfamily}
\begin{lstlisting}[frame=single, ]  
int w, x, y, z;
int i = 4; int j = 5;
{   int j = 7;
    i = 6;
    w = i + j;
}
x = i + j;
{   int i = 8;
    y = i + j;
}
z = i + j;
\end{lstlisting}
\normalsize{w = 13}

\normalsize{x = 11}

\normalsize{y = 13}

\normalsize{z = 11}

\end{document}
