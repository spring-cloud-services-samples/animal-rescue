import React from 'react'
import {Card} from 'semantic-ui-react'
import AnimalCard from "./animal-card";

const AnimalCards = (props) => (
    <Card.Group>
        {
            props.animals.map(animal => (
                <AnimalCard animal={animal} key={animal.id}/>
            ))
        }
    </Card.Group>
);

export default AnimalCards
