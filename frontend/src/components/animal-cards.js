import React from 'react';
import {Card} from 'semantic-ui-react';
import AnimalCard from "./animal-card";
import * as PropTypes from "prop-types";

export default class AnimalCards extends React.Component {

    render() {
        const cards = this.props.animals.map(animal => (
            <AnimalCard animal={animal}
                        key={animal.id}
                        username={this.props.username}/>
        ));
        return (
            <Card.Group centered>
                {cards}
            </Card.Group>
        );
    }
}

AnimalCards.propTypes = {
    username: PropTypes.string.isRequired,
    animals: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number.isRequired,
    })),
};
