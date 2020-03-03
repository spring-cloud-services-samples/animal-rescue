import React from "react";
import { Carousel } from "react-responsive-carousel";

const images = [
    "/carousel-1.jpg",
    "/carousel-2.jpg",
    "/carousel-3.jpg",
    "/carousel-4.jpg",
    "/carousel-5.jpg",
];

export default () => (
  <Carousel infiniteLoop showThumbs={false} showStatus={false} autoPlay showIndicators={false}>
      {
          images.map((image, i) => (
              <div key={i}>
                  <img src={process.env.PUBLIC_URL + image} alt={`cute-animal-${i}`} />
              </div>
          ))
      }
  </Carousel>
);
