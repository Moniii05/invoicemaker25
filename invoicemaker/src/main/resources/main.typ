#import "invoice-maker.typ": invoice

#set page(paper: "a4", margin: 18mm)
#let data = json("data.json")

#show: invoice.with(
  banner-image: image("banner.png"),
  ..data
)
