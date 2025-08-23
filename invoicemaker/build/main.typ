#import "@preview/invoice-maker:1.1.0": invoice

#set page(paper: "a4", margin: 18mm)
#let data = json("data.json")

#image("banner.png", width: 160mm)
#v(6mm)

#show: invoice(data: data)[]

