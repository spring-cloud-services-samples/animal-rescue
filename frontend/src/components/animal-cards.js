import React from 'react'
import {Card} from 'semantic-ui-react'
import AnimalCard from "./animal-card";
import HttpClient from "../httpClient";
import * as PropTypes from "prop-types";

export default class AnimalCards extends React.Component {

    constructor(props, context) {
        super(props, context);
        this.state = {
            animals: [],
        };
    }

    componentDidMount() {
        this.props.httpClient
            .getAnimals()
            .then(animals => this.setState({animals}));
    }

    render() {
        const cards = this.state.animals.map(animal => (
            <AnimalCard animal={animal}
                        key={animal.id}
                        username={this.props.username}
                        httpClient={this.props.httpClient}/>
        ));
        return (
            <Card.Group centered>
                {cards}
            </Card.Group>
        )
    }
}

AnimalCards.propTypes = {
    username: PropTypes.string.isRequired,
    httpClient: PropTypes.instanceOf(HttpClient).isRequired,
};
