import React from 'react'
import {Card} from 'semantic-ui-react'
import AnimalCard from "./animal-card";
import HttpClient from "../httpClient";

export default class AnimalCards extends React.Component {

    constructor(props, context) {
        super(props, context);
        this.httpClient = new HttpClient();
        this.state = {
            animals: []
        };
    }

    componentDidMount() {
        this.httpClient
            .getAnimals()
            .then(animals => this.setState({animals}));
    }

    render() {
        const cards = this.state.animals.map(animal => (
            <AnimalCard animal={animal} key={animal.id} httpClient={this.httpClient}/>
        ));
        return (
            <Card.Group>
                {cards}
            </Card.Group>
        )
    }
}

