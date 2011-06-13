#!/bin/sh
latex s22.tex
latex s23.tex
latex s33.tex
latex s34.tex
latex s44.tex

latex s22single.tex
latex s23single.tex
latex s33single.tex
latex s34single.tex
latex s44single.tex

xdvi s22.dvi &
xdvi s23.dvi &
xdvi s33.dvi &
xdvi s34.dvi &
xdvi s44.dvi &

xdvi s22single.dvi &
xdvi s23single.dvi &
xdvi s33single.dvi &
xdvi s34single.dvi &
xdvi s44single.dvi &
